package com.loveeleve.ecology.budget.service;

import com.loveeleve.ecology.budget.config.BudgetProperties;
import com.loveeleve.ecology.budget.domain.BudgetAccountKey;
import com.loveeleve.ecology.budget.domain.BudgetFlow;
import com.loveeleve.ecology.budget.domain.BudgetFlowStatus;
import com.loveeleve.ecology.budget.domain.BudgetLease;
import com.loveeleve.ecology.budget.domain.BudgetUnit;
import com.loveeleve.ecology.budget.domain.LeaseRequest;
import com.loveeleve.ecology.budget.domain.LeaseStatus;
import com.loveeleve.ecology.budget.domain.ReserveRequest;
import com.loveeleve.ecology.budget.domain.ReserveResult;
import com.loveeleve.ecology.budget.exception.BudgetException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 生产口径的预算预扣服务（Spring Boot + JDBC + MySQL/H2）。
 *
 * <p>关键点：</p>
 * <ul>
 *     <li>余额扣减用条件 UPDATE，影响行数 0 即预算不足；</li>
 *     <li>总预算/子预算/来源在同一个本地事务中扣减，任一失败整体回滚；</li>
 *     <li>requestId 唯一键做预占幂等；txnId 状态条件做 confirm/release 幂等；</li>
 *     <li>号段申请用 leaseRequestId 唯一键做幂等；</li>
 *     <li>多来源逐来源尝试，每个来源独立事务，失败可继续下一个来源。</li>
 * </ul>
 *
 * <p>生产落库表：t_budget_account、t_budget_flow、t_budget_lease。
 * 真实环境建议使用 MySQL，单元测试可用 H2 MySQL 模式。</p>
 */
@Service
public class BudgetJdbcService {

    private static final String TOTAL_TEMPLATE = BudgetAccountKey.TOTAL_TEMPLATE;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final BudgetProperties properties;

    public BudgetJdbcService(JdbcTemplate jdbc, TransactionTemplate tx, BudgetProperties properties) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.properties = properties;
    }

    /**
     * 预占：按来源优先级逐个尝试。
     *
     * <p>每个来源一个独立事务。某个来源预算不足时，该事务回滚，继续尝试下一个来源；
     * 不会因为首选来源不足而影响备选来源。</p>
     */
    public ReserveResult reserve(ReserveRequest request) {
        BudgetFlow existing = findFlowByRequestId(request.requestId());
        if (existing != null) {
            return replay(existing, request);
        }

        BudgetException lastInsufficient = null;
        for (String sourceId : request.sourceCandidates()) {
            try {
                BudgetFlow flow = tx.execute(status -> reserveOne(request, sourceId));
                return ReserveResult.existing(flow);
            } catch (BudgetException ex) {
                if (ex.code() == BudgetException.Code.INSUFFICIENT_BUDGET) {
                    lastInsufficient = ex;
                    continue;
                }
                throw ex;
            } catch (DuplicateKeyException ex) {
                // 并发同 requestId：另一个事务先插入成功；回查并返回已有结果。
                BudgetFlow replay = findFlowByRequestId(request.requestId());
                if (replay != null) {
                    return replay(replay, request);
                }
                throw ex;
            }
        }
        if (lastInsufficient != null) {
            throw lastInsufficient;
        }
        throw new BudgetException(BudgetException.Code.INSUFFICIENT_BUDGET,
                "no source available for request " + request.requestId());
    }

    private BudgetFlow reserveOne(ReserveRequest request, String sourceId) {
        Instant now = Instant.now();
        String txnId = UUID.randomUUID().toString();

        // 1. 先插入流水，靠 request_id 唯一键抢占幂等；后续扣减失败会随事务回滚。
        jdbc.update("""
                        INSERT INTO t_budget_flow
                        (txn_id, request_id, request_hash, activity_id, source_id, template_id,
                         unit, amount, lease_id, status, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, 'RESERVED', ?, ?)
                        """,
                txnId, request.requestId(), request.requestHash(), request.activityId(),
                sourceId, request.templateId(), request.unit().name(), request.amount(),
                Timestamp.from(now), Timestamp.from(now));

        // 2. 扣来源池总额：条件更新，available 不足返回 0。
        int totalUpdated = jdbc.update("""
                        UPDATE t_budget_account
                        SET reserved_amount = reserved_amount + ?, updated_at = ?
                        WHERE activity_id = ? AND source_id = ? AND template_id = ? AND unit = ?
                          AND total_amount - reserved_amount - consumed_amount >= ?
                        """,
                request.amount(), Timestamp.from(now), request.activityId(), sourceId,
                TOTAL_TEMPLATE, request.unit().name(), request.amount());
        requireUpdated(totalUpdated, request, "source total");

        // 3. 扣券模板子预算；失败会抛异常，整个事务回滚，总预算不会留下半扣。
        if (!TOTAL_TEMPLATE.equals(request.templateId())) {
            int subUpdated = jdbc.update("""
                            UPDATE t_budget_account
                            SET reserved_amount = reserved_amount + ?, updated_at = ?
                            WHERE activity_id = ? AND source_id = ? AND template_id = ? AND unit = ?
                              AND total_amount - reserved_amount - consumed_amount >= ?
                            """,
                    request.amount(), Timestamp.from(now), request.activityId(), sourceId,
                    request.templateId(), request.unit().name(), request.amount());
            requireUpdated(subUpdated, request, "template sub budget");
        }

        return findFlowByTxnId(txnId);
    }

    /** 确认预占；重复 confirm 幂等返回。 */
    @Transactional
    public BudgetFlow confirm(String txnId) {
        BudgetFlow flow = findFlowForUpdate(txnId);
        if (flow.status() == BudgetFlowStatus.CONFIRMED) {
            return flow;
        }
        if (flow.status() == BudgetFlowStatus.RELEASED) {
            throw new BudgetException(BudgetException.Code.FLOW_STATE_CONFLICT,
                    "flow already released: " + txnId);
        }

        int updated = jdbc.update("""
                        UPDATE t_budget_flow
                        SET status = 'CONFIRMED', updated_at = ?
                        WHERE txn_id = ? AND status = 'RESERVED'
                        """, Timestamp.from(Instant.now()), txnId);
        if (updated != 1) {
            throw new BudgetException(BudgetException.Code.FLOW_STATE_CONFLICT,
                    "flow state changed: " + txnId);
        }

        moveReservedToConsumed(flow);
        return findFlowByTxnId(txnId);
    }

    /** 释放预占；重复 release 幂等返回。 */
    public BudgetFlow release(String txnId) {
        return tx.execute(status -> releaseInternal(txnId));
    }

    private BudgetFlow releaseInternal(String txnId) {
        BudgetFlow flow = findFlowForUpdate(txnId);
        if (flow.status() == BudgetFlowStatus.RELEASED) {
            return flow;
        }
        if (flow.status() == BudgetFlowStatus.CONFIRMED) {
            throw new BudgetException(BudgetException.Code.FLOW_STATE_CONFLICT,
                    "flow already confirmed: " + txnId);
        }

        int updated = jdbc.update("""
                        UPDATE t_budget_flow
                        SET status = 'RELEASED', updated_at = ?
                        WHERE txn_id = ? AND status = 'RESERVED'
                        """, Timestamp.from(Instant.now()), txnId);
        if (updated != 1) {
            throw new BudgetException(BudgetException.Code.FLOW_STATE_CONFLICT,
                    "flow state changed: " + txnId);
        }

        releaseReserved(flow);
        if (flow.leaseId() != null) {
            jdbc.update("""
                            UPDATE t_budget_lease
                            SET used_amount = used_amount - ?, updated_at = ?
                            WHERE lease_id = ? AND used_amount >= ?
                            """, flow.amount(), Timestamp.from(Instant.now()), flow.leaseId(), flow.amount());
        }
        return findFlowByTxnId(txnId);
    }

    /** 追加预算；只增不减。 */
    @Transactional
    public void append(BudgetAccountKey key, BigDecimal delta) {
        if (delta == null || delta.signum() <= 0) {
            throw new BudgetException(BudgetException.Code.INVALID_ARGUMENT, "delta must be > 0");
        }
        int updated = jdbc.update("""
                        UPDATE t_budget_account
                        SET total_amount = total_amount + ?, budget_version = budget_version + 1, updated_at = ?
                        WHERE activity_id = ? AND source_id = ? AND template_id = ? AND unit = ?
                        """, delta, Timestamp.from(Instant.now()), key.activityId(), key.sourceId(),
                key.templateId(), key.unit().name());
        if (updated != 1) {
            throw new BudgetException(BudgetException.Code.BUDGET_NOT_FOUND,
                    "budget account not found: " + key);
        }
    }

    /**
     * 超时回收：扫 RESERVED 且 created_at 过旧的流水，逐条 release。
     *
     * <p>生产可换 XXL-Job + SELECT ... FOR UPDATE SKIP LOCKED；本示例用 limit
     * 和状态条件更新保证幂等。</p>
     */
    public int expireReservations(Instant now, Duration maxHold) {
        Timestamp before = Timestamp.from(now.minus(maxHold));
        List<String> txnIds = jdbc.queryForList("""
                        SELECT txn_id FROM t_budget_flow
                        WHERE status = 'RESERVED' AND created_at < ?
                        ORDER BY created_at
                        LIMIT ?
                        """, String.class, before, properties.getExpiryScanLimit());

        int released = 0;
        for (String txnId : txnIds) {
            try {
                tx.execute(status -> releaseInternal(txnId));
                released++;
            } catch (BudgetException ex) {
                // 已被 confirm/release：正常竞争，跳过。
            }
        }
        return released;
    }

    /**
     * 申请本地号段：来源池总额 + 子预算同时预占；leaseRequestId 唯一键幂等。
     *
     * <p>用 TransactionTemplate 而不是 @Transactional，原因是要在事务回滚后
     * 捕获 lease_request_id 唯一键冲突，再回查已有租约，避免响应丢失后的
     * 重复申请。</p>
     */
    public BudgetLease allocateLease(LeaseRequest request) {
        try {
            return tx.execute(status -> allocateLeaseInternal(request));
        } catch (DuplicateKeyException ex) {
            BudgetLease existing = findLeaseByRequestId(request.leaseRequestId());
            if (existing != null) {
                return existing;
            }
            throw ex;
        }
    }

    private BudgetLease allocateLeaseInternal(LeaseRequest request) {
        BudgetLease existing = findLeaseByRequestId(request.leaseRequestId());
        if (existing != null) {
            return existing;
        }

        Instant now = Instant.now();
        String leaseId = UUID.randomUUID().toString();

        int totalUpdated = jdbc.update("""
                        UPDATE t_budget_account
                        SET reserved_amount = reserved_amount + ?, updated_at = ?
                        WHERE activity_id = ? AND source_id = ? AND template_id = ? AND unit = ?
                          AND total_amount - reserved_amount - consumed_amount >= ?
                        """, request.chunk(), Timestamp.from(now), request.activityId(), request.sourceId(),
                TOTAL_TEMPLATE, request.unit().name(), request.chunk());
        requireLeaseUpdated(totalUpdated, request, "source total");

        if (!TOTAL_TEMPLATE.equals(request.templateId())) {
            int subUpdated = jdbc.update("""
                            UPDATE t_budget_account
                            SET reserved_amount = reserved_amount + ?, updated_at = ?
                            WHERE activity_id = ? AND source_id = ? AND template_id = ? AND unit = ?
                              AND total_amount - reserved_amount - consumed_amount >= ?
                            """, request.chunk(), Timestamp.from(now), request.activityId(), request.sourceId(),
                    request.templateId(), request.unit().name(), request.chunk());
            requireLeaseUpdated(subUpdated, request, "template sub budget");
        }

        jdbc.update("""
                        INSERT INTO t_budget_lease
                        (lease_id, lease_request_id, instance_id, activity_id, source_id, template_id,
                         unit, chunk_amount, used_amount, status, expire_at, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 'ACTIVE', ?, ?, ?)
                        """, leaseId, request.leaseRequestId(), request.instanceId(), request.activityId(),
                request.sourceId(), request.templateId(), request.unit().name(), request.chunk(),
                Timestamp.from(request.expireAt()), Timestamp.from(now), Timestamp.from(now));

        return findLeaseById(leaseId);
    }

    /**
     * 从本地号段预占：只消耗 lease，不再改中央账户。
     */
    @Transactional
    public BudgetFlow reserveFromLease(ReserveRequest request, String leaseId) {
        BudgetFlow existing = findFlowByRequestId(request.requestId());
        if (existing != null) {
            if (!existing.requestHash().equals(request.requestHash())) {
                throw new BudgetException(BudgetException.Code.IDEMPOTENCY_CONFLICT,
                        "requestId reused with different params: " + request.requestId());
            }
            return existing;
        }

        int used = jdbc.update("""
                        UPDATE t_budget_lease
                        SET used_amount = used_amount + ?, updated_at = ?
                        WHERE lease_id = ? AND status = 'ACTIVE'
                          AND chunk_amount - used_amount >= ?
                        """, request.amount(), Timestamp.from(Instant.now()), leaseId, request.amount());
        if (used != 1) {
            throw new BudgetException(BudgetException.Code.LEASE_EXHAUSTED,
                    "lease unavailable: " + leaseId);
        }

        Instant now = Instant.now();
        jdbc.update("""
                        INSERT INTO t_budget_flow
                        (txn_id, request_id, request_hash, activity_id, source_id, template_id,
                         unit, amount, lease_id, status, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'RESERVED', ?, ?)
                        """, UUID.randomUUID().toString(), request.requestId(), request.requestHash(),
                request.activityId(), findLeaseById(leaseId).key().sourceId(), request.templateId(),
                request.unit().name(), request.amount(), leaseId, Timestamp.from(now), Timestamp.from(now));

        return findFlowByRequestId(request.requestId());
    }

    /** 归还号段未用额度；已用部分保持。 */
    @Transactional
    public void returnLease(String leaseId) {
        BudgetLease lease = findLeaseForUpdate(leaseId);
        if (lease.status() != LeaseStatus.ACTIVE) {
            return;
        }
        BigDecimal unused = lease.remaining();
        if (unused.signum() > 0) {
            jdbc.update("""
                            UPDATE t_budget_account
                            SET reserved_amount = reserved_amount - ?, updated_at = ?
                            WHERE activity_id = ? AND source_id = ? AND template_id = ? AND unit = ?
                            """, unused, Timestamp.from(Instant.now()), lease.key().activityId(),
                    lease.key().sourceId(), TOTAL_TEMPLATE, lease.key().unit().name());

            if (!TOTAL_TEMPLATE.equals(lease.key().templateId())) {
                jdbc.update("""
                                UPDATE t_budget_account
                                SET reserved_amount = reserved_amount - ?, updated_at = ?
                                WHERE activity_id = ? AND source_id = ? AND template_id = ? AND unit = ?
                                """, unused, Timestamp.from(Instant.now()), lease.key().activityId(),
                        lease.key().sourceId(), lease.key().templateId(), lease.key().unit().name());
            }
        }
        jdbc.update("""
                        UPDATE t_budget_lease SET status = 'RETURNED', updated_at = ?
                        WHERE lease_id = ? AND status = 'ACTIVE'
                        """, Timestamp.from(Instant.now()), leaseId);
    }

    /** 查询某个活动的预算账户，供运营/对账使用。 */
    public List<com.loveeleve.ecology.budget.api.BudgetAccountView> findAccounts(String activityId) {
        return jdbc.query("""
                        SELECT activity_id, source_id, template_id, unit,
                               total_amount, reserved_amount, consumed_amount,
                               total_amount - reserved_amount - consumed_amount AS available,
                               budget_version
                        FROM t_budget_account
                        WHERE activity_id = ?
                        ORDER BY source_id, template_id
                        """,
                (rs, rowNum) -> new com.loveeleve.ecology.budget.api.BudgetAccountView(
                        rs.getString("activity_id"),
                        rs.getString("source_id"),
                        rs.getString("template_id"),
                        rs.getString("unit"),
                        rs.getBigDecimal("total_amount"),
                        rs.getBigDecimal("reserved_amount"),
                        rs.getBigDecimal("consumed_amount"),
                        rs.getBigDecimal("available"),
                        rs.getLong("budget_version")),
                activityId);
    }

    public BudgetFlow findFlowByTxnId(String txnId) {
        return jdbc.query("SELECT * FROM t_budget_flow WHERE txn_id = ?", flowMapper(), txnId)
                .stream().findFirst().orElseThrow(() -> new BudgetException(
                        BudgetException.Code.FLOW_NOT_FOUND, "flow not found: " + txnId));
    }

    public BudgetFlow findFlowByRequestId(String requestId) {
        return jdbc.query("SELECT * FROM t_budget_flow WHERE request_id = ?", flowMapper(), requestId)
                .stream().findFirst().orElse(null);
    }

    public BudgetLease findLeaseByRequestId(String leaseRequestId) {
        return jdbc.query("SELECT * FROM t_budget_lease WHERE lease_request_id = ?", leaseMapper(), leaseRequestId)
                .stream().findFirst().orElse(null);
    }

    public BudgetLease findLeaseById(String leaseId) {
        return jdbc.query("SELECT * FROM t_budget_lease WHERE lease_id = ?", leaseMapper(), leaseId)
                .stream().findFirst().orElseThrow(() -> new BudgetException(
                        BudgetException.Code.LEASE_NOT_FOUND, "lease not found: " + leaseId));
    }

    private BudgetLease findLeaseForUpdate(String leaseId) {
        return jdbc.query("SELECT * FROM t_budget_lease WHERE lease_id = ? FOR UPDATE", leaseMapper(), leaseId)
                .stream().findFirst().orElseThrow(() -> new BudgetException(
                        BudgetException.Code.LEASE_NOT_FOUND, "lease not found: " + leaseId));
    }

    private BudgetFlow findFlowForUpdate(String txnId) {
        return jdbc.query("SELECT * FROM t_budget_flow WHERE txn_id = ? FOR UPDATE", flowMapper(), txnId)
                .stream().findFirst().orElseThrow(() -> new BudgetException(
                        BudgetException.Code.FLOW_NOT_FOUND, "flow not found: " + txnId));
    }

    private void moveReservedToConsumed(BudgetFlow flow) {
        int total = jdbc.update("""
                        UPDATE t_budget_account
                        SET reserved_amount = reserved_amount - ?,
                            consumed_amount = consumed_amount + ?,
                            updated_at = ?
                        WHERE activity_id = ? AND source_id = ? AND template_id = ? AND unit = ?
                          AND reserved_amount >= ?
                        """, flow.amount(), flow.amount(), Timestamp.from(Instant.now()),
                flow.activityId(), flow.sourceId(), TOTAL_TEMPLATE, flow.unit().name(), flow.amount());
        requireAccountUpdated(total, flow, "confirm source total");

        if (!TOTAL_TEMPLATE.equals(flow.templateId())) {
            int sub = jdbc.update("""
                            UPDATE t_budget_account
                            SET reserved_amount = reserved_amount - ?,
                                consumed_amount = consumed_amount + ?,
                                updated_at = ?
                            WHERE activity_id = ? AND source_id = ? AND template_id = ? AND unit = ?
                              AND reserved_amount >= ?
                            """, flow.amount(), flow.amount(), Timestamp.from(Instant.now()),
                    flow.activityId(), flow.sourceId(), flow.templateId(), flow.unit().name(), flow.amount());
            requireAccountUpdated(sub, flow, "confirm template sub budget");
        }
    }

    private void releaseReserved(BudgetFlow flow) {
        int total = jdbc.update("""
                        UPDATE t_budget_account
                        SET reserved_amount = reserved_amount - ?, updated_at = ?
                        WHERE activity_id = ? AND source_id = ? AND template_id = ? AND unit = ?
                          AND reserved_amount >= ?
                        """, flow.amount(), Timestamp.from(Instant.now()), flow.activityId(),
                flow.sourceId(), TOTAL_TEMPLATE, flow.unit().name(), flow.amount());
        requireAccountUpdated(total, flow, "release source total");

        if (!TOTAL_TEMPLATE.equals(flow.templateId())) {
            int sub = jdbc.update("""
                            UPDATE t_budget_account
                            SET reserved_amount = reserved_amount - ?, updated_at = ?
                            WHERE activity_id = ? AND source_id = ? AND template_id = ? AND unit = ?
                              AND reserved_amount >= ?
                            """, flow.amount(), Timestamp.from(Instant.now()), flow.activityId(),
                    flow.sourceId(), flow.templateId(), flow.unit().name(), flow.amount());
            requireAccountUpdated(sub, flow, "release template sub budget");
        }
    }

    private ReserveResult replay(BudgetFlow existing, ReserveRequest request) {
        if (!existing.requestHash().equals(request.requestHash())) {
            throw new BudgetException(BudgetException.Code.IDEMPOTENCY_CONFLICT,
                    "requestId reused with different params: " + request.requestId());
        }
        return ReserveResult.existing(existing);
    }

    private void requireUpdated(int updated, ReserveRequest request, String layer) {
        if (updated != 1) {
            throw new BudgetException(BudgetException.Code.INSUFFICIENT_BUDGET,
                    "insufficient " + layer + " for request " + request.requestId());
        }
    }

    private void requireLeaseUpdated(int updated, LeaseRequest request, String layer) {
        if (updated != 1) {
            throw new BudgetException(BudgetException.Code.INSUFFICIENT_BUDGET,
                    "insufficient " + layer + " for lease " + request.leaseRequestId());
        }
    }

    private void requireAccountUpdated(int updated, BudgetFlow flow, String action) {
        if (updated != 1) {
            throw new BudgetException(BudgetException.Code.FLOW_STATE_CONFLICT,
                    action + " failed for flow " + flow.txnId());
        }
    }

    private RowMapper<BudgetFlow> flowMapper() {
        return (rs, rowNum) -> new BudgetFlow(
                rs.getString("txn_id"),
                rs.getString("request_id"),
                rs.getString("request_hash"),
                rs.getString("activity_id"),
                rs.getString("source_id"),
                rs.getString("template_id"),
                BudgetUnit.valueOf(rs.getString("unit")),
                rs.getBigDecimal("amount"),
                rs.getString("lease_id"),
                toInstant(rs, "created_at"),
                BudgetFlowStatus.valueOf(rs.getString("status")),
                toInstant(rs, "updated_at"));
    }

    private RowMapper<BudgetLease> leaseMapper() {
        return (rs, rowNum) -> new BudgetLease(
                rs.getString("lease_id"),
                rs.getString("lease_request_id"),
                rs.getString("instance_id"),
                BudgetAccountKey.sub(
                        rs.getString("activity_id"),
                        rs.getString("source_id"),
                        rs.getString("template_id"),
                        BudgetUnit.valueOf(rs.getString("unit"))),
                rs.getBigDecimal("chunk_amount"),
                rs.getBigDecimal("used_amount"),
                LeaseStatus.valueOf(rs.getString("status")),
                toInstant(rs, "expire_at"));
    }

    private Instant toInstant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}

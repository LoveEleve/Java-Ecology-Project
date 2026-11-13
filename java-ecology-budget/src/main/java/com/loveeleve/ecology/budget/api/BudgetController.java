package com.loveeleve.ecology.budget.api;

import com.loveeleve.ecology.budget.config.BudgetProperties;
import com.loveeleve.ecology.budget.domain.BudgetFlow;
import com.loveeleve.ecology.budget.domain.BudgetLease;
import com.loveeleve.ecology.budget.domain.BudgetUnit;
import com.loveeleve.ecology.budget.domain.ReserveRequest;
import com.loveeleve.ecology.budget.service.BudgetJdbcService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 预算预扣服务 HTTP 入口。
 *
 * <p>接口只暴露预算能力，不暴露券、核销、归因：
 * reserve / confirm / release / append / lease / query。</p>
 */
@RestController
@RequestMapping("/api/budget")
public class BudgetController {

    private final BudgetJdbcService service;
    private final BudgetProperties properties;

    public BudgetController(BudgetJdbcService service, BudgetProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    /** 预占预算。 */
    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReserveApiResponse reserve(@Valid @RequestBody ReserveApiRequest request) {
        return ReserveApiResponse.from(service.reserve(request.toDomain()));
    }

    /** 确认预占。 */
    @PostMapping("/reservations/{txnId}/confirm")
    public BudgetFlowView confirm(@PathVariable String txnId) {
        return BudgetFlowView.from(service.confirm(txnId));
    }

    /** 释放预占。 */
    @PostMapping("/reservations/{txnId}/release")
    public BudgetFlowView release(@PathVariable String txnId) {
        return BudgetFlowView.from(service.release(txnId));
    }

    /** 追加预算；只增不减。 */
    @PostMapping("/accounts/append")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void append(@Valid @RequestBody AppendApiRequest request) {
        service.append(request.toKey(), request.delta());
    }

    /** 查询活动预算账户。 */
    @GetMapping("/accounts")
    public List<BudgetAccountView> accounts(@RequestParam String activityId) {
        return service.findAccounts(activityId);
    }

    /** 查询单笔流水。 */
    @GetMapping("/flows/{txnId}")
    public BudgetFlowView flow(@PathVariable String txnId) {
        BudgetFlow flow = service.findFlowByTxnId(txnId);
        return BudgetFlowView.from(flow);
    }

    /** 申请本地号段。 */
    @PostMapping("/leases")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaseView allocateLease(@Valid @RequestBody LeaseApiRequest request) {
        BudgetLease lease = service.allocateLease(request.toDomain(properties.getLeaseTtl()));
        return LeaseView.from(lease);
    }

    /** 从本地号段预占。 */
    @PostMapping("/leases/{leaseId}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReserveApiResponse reserveFromLease(
            @PathVariable String leaseId,
            @Valid @RequestBody ReserveApiRequest request) {
        ReserveRequest domain = request.toDomain();
        BudgetFlow flow = service.reserveFromLease(domain, leaseId);
        return new ReserveApiResponse(flow.txnId(), flow.sourceId(),
                flow.status().name(), "reserved from lease");
    }

    /** 归还号段未用额度。 */
    @PostMapping("/leases/{leaseId}/return")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void returnLease(@PathVariable String leaseId) {
        service.returnLease(leaseId);
    }

    /** 号段视图。 */
    public record LeaseView(
            String leaseId,
            String instanceId,
            String activityId,
            String sourceId,
            String templateId,
            String unit,
            BigDecimal chunk,
            BigDecimal used,
            BigDecimal remaining,
            String status) {

        static LeaseView from(BudgetLease lease) {
            return new LeaseView(
                    lease.leaseId(), lease.instanceId(), lease.key().activityId(),
                    lease.key().sourceId(), lease.key().templateId(),
                    lease.key().unit().name(), lease.chunk(), lease.used(),
                    lease.remaining(), lease.status().name());
        }
    }
}

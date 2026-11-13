package com.loveeleve.ecology.budget.lease;

import com.loveeleve.ecology.budget.domain.BudgetAccountKey;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 本地号段槽位。
 *
 * <p>一个槽位对应一个 activity + source + template 的本地额度；
 * 多个子预算各自一个槽位，总来源额度由中央账户统一控制。</p>
 */
public final class LeaseSlot {

    private final BudgetAccountKey key;
    private final AtomicReference<BigDecimal> remaining = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<LeaseState> state = new AtomicReference<>(LeaseState.EXHAUSTED);
    private final EwmaRate rate = new EwmaRate(30);
    private final AdaptiveChunkSizer sizer = new AdaptiveChunkSizer(100, 100_000, 0.05, 2.0);

    private volatile String leaseId;
    private volatile BigDecimal chunk = BigDecimal.ZERO;
    private volatile long lastUsedAtNanos = System.nanoTime();
    private volatile long nextProbeAtNanos;

    public LeaseSlot(BudgetAccountKey key) {
        this.key = key;
    }

    public BudgetAccountKey key() {
        return key;
    }

    public String leaseId() {
        return leaseId;
    }

    public BigDecimal chunk() {
        return chunk;
    }

    public BigDecimal remaining() {
        return remaining.get();
    }

    public LeaseState state() {
        return state.get();
    }

    public EwmaRate rate() {
        return rate;
    }

    public AdaptiveChunkSizer sizer() {
        return sizer;
    }

    public long lastUsedAtNanos() {
        return lastUsedAtNanos;
    }

    public long nextProbeAtNanos() {
        return nextProbeAtNanos;
    }

    /** 尝试从本地号段预占；CAS 保证不会超领。 */
    public boolean tryAcquire(BigDecimal amount) {
        while (true) {
            BigDecimal current = remaining.get();
            if (current.compareTo(amount) < 0) {
                return false;
            }
            if (remaining.compareAndSet(current, current.subtract(amount))) {
                lastUsedAtNanos = System.nanoTime();
                rate.record(amount.longValue(), System.nanoTime());
                return true;
            }
        }
    }

    /** 请求释放后，把额度还回本地号段。 */
    public void release(BigDecimal amount) {
        remaining.updateAndGet(current -> current.add(amount));
    }

    /** 安装新号段：新段准备好后原子切换。 */
    public void installLease(String leaseId, BigDecimal chunk) {
        this.leaseId = leaseId;
        this.chunk = chunk;
        this.remaining.set(chunk);
        this.state.set(LeaseState.ACTIVE);
        this.nextProbeAtNanos = 0;
    }

    public boolean casState(LeaseState expect, LeaseState update) {
        return state.compareAndSet(expect, update);
    }

    public void state(LeaseState next) {
        state.set(next);
    }

    public void nextProbeAtNanos(long value) {
        nextProbeAtNanos = value;
    }
}

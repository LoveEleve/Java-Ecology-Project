package com.loveeleve.ecology.budget.lease;

/**
 * 本地号段状态。
 *
 * <pre>
 * ACTIVE      本地有额度，可正常扣
 * REFILLING   有且只有一个线程在申请/探测 DB
 * DRAINING    DB 已确认不足，但本地还有额度，继续消耗但不续段
 * EXHAUSTED   本地也没额度，快速失败
 * BACKOFF     DB 技术失败，退避
 * TERMINAL    活动结束/预算关闭，永久停止
 * </pre>
 */
public enum LeaseState {
    ACTIVE,
    REFILLING,
    DRAINING,
    EXHAUSTED,
    BACKOFF,
    TERMINAL
}

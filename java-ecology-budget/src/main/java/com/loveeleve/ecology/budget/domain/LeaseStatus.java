package com.loveeleve.ecology.budget.domain;

/**
 * 中央发给实例的预算号段状态。
 */
public enum LeaseStatus {
    /** 可被本地消费。 */
    ACTIVE,
    /** 已归还中央。 */
    RETURNED,
    /** 已过期/被回收。 */
    EXPIRED
}

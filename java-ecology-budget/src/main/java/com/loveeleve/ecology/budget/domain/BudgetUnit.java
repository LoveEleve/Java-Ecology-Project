package com.loveeleve.ecology.budget.domain;

/**
 * 预算单位。
 *
 * <p>金额预算和数量预算不能混在同一个预算账户里：</p>
 * <ul>
 *     <li>{@link #AMOUNT}：按金额控制，例如 100 万元；</li>
 *     <li>{@link #QUANTITY}：按数量控制，例如 10 万张券。</li>
 * </ul>
 *
 * <p>如果业务既要控成本又要控库存，应该建立两个预算账户，发券时同时预占。</p>
 */
public enum BudgetUnit {
    AMOUNT,
    QUANTITY
}

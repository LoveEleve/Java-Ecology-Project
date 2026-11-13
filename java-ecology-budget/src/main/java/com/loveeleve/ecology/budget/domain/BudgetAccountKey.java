package com.loveeleve.ecology.budget.domain;

/**
 * 预算账户的唯一键。
 *
 * <p>一个预算账户由四个维度确定：</p>
 * <pre>
 * activityId + sourceId + templateId + unit
 * </pre>
 *
 * <p>其中：</p>
 * <ul>
 *     <li>{@code activityId}：活动；</li>
 *     <li>{@code sourceId}：资金来源，例如市场部、产品部；</li>
 *     <li>{@code templateId}：券模板；</li>
 *     <li>{@code unit}：金额/数量。</li>
 * </ul>
 *
 * <p>来源池总额使用 {@link #TOTAL_TEMPLATE} 作为模板占位符；
 * 具体券模板子预算使用真实 templateId。</p>
 */
public record BudgetAccountKey(
        String activityId,
        String sourceId,
        String templateId,
        BudgetUnit unit) {

    /** 来源池总额账户的模板占位符。 */
    public static final String TOTAL_TEMPLATE = "__TOTAL__";

    public BudgetAccountKey {
        requireText(activityId, "activityId");
        requireText(sourceId, "sourceId");
        requireText(templateId, "templateId");
        if (unit == null) {
            throw new IllegalArgumentException("unit must not be null");
        }
    }

    /** 来源池总额账户。 */
    public static BudgetAccountKey total(String activityId, String sourceId, BudgetUnit unit) {
        return new BudgetAccountKey(activityId, sourceId, TOTAL_TEMPLATE, unit);
    }

    /** 某个券模板的子预算账户。 */
    public static BudgetAccountKey sub(String activityId, String sourceId, String templateId, BudgetUnit unit) {
        return new BudgetAccountKey(activityId, sourceId, templateId, unit);
    }

    /** 是否是来源池总额账户。 */
    public boolean isTotal() {
        return TOTAL_TEMPLATE.equals(templateId);
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}

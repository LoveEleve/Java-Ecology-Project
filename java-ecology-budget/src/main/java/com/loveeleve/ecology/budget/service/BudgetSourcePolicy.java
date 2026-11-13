package com.loveeleve.ecology.budget.service;

import com.loveeleve.ecology.budget.domain.ReserveRequest;

import java.util.List;

/**
 * 资金来源选择策略。
 *
 * <p>生产中可以按用户渠道、活动配置、来源优先级、权重等实现；
 * 本示例默认按请求里的 sourceCandidates 顺序尝试。</p>
 */
public interface BudgetSourcePolicy {

    /** 返回本次请求允许尝试的来源，顺序即优先级。 */
    List<String> candidates(ReserveRequest request);
}

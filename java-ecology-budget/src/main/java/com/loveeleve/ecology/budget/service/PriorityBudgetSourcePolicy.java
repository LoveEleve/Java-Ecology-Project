package com.loveeleve.ecology.budget.service;

import com.loveeleve.ecology.budget.domain.ReserveRequest;

import java.util.List;

/**
 * 默认来源策略：按请求中 sourceCandidates 的顺序瀑布尝试。
 *
 * <p>真实中台里 sourceCandidates 一般来自活动配置 + 用户渠道匹配，
 * 不应由业务方随意传入。</p>
 */
public final class PriorityBudgetSourcePolicy implements BudgetSourcePolicy {

    @Override
    public List<String> candidates(ReserveRequest request) {
        return request.sourceCandidates();
    }
}

package com.loveeleve.ecology.budget.exception;

/**
 * 预算服务统一业务异常。
 *
 * <p>用错误码区分“业务失败”和“技术失败”：</p>
 * <ul>
 *     <li>INSUFFICIENT_BUDGET：预算真的不足，快速失败；</li>
 *     <li>IDEMPOTENCY_CONFLICT：同 requestId 参数不一致；</li>
 *     <li>FLOW_STATE_CONFLICT：重复 confirm/release 的非法状态；</li>
 *     <li>LEASE_STATE_CONFLICT：号段状态不允许当前操作。</li>
 * </ul>
 */
public class BudgetException extends RuntimeException {

    public enum Code {
        INVALID_ARGUMENT,
        BUDGET_NOT_FOUND,
        INSUFFICIENT_BUDGET,
        IDEMPOTENCY_CONFLICT,
        FLOW_NOT_FOUND,
        FLOW_STATE_CONFLICT,
        LEASE_NOT_FOUND,
        LEASE_STATE_CONFLICT,
        LEASE_EXHAUSTED,
        INTERNAL_ERROR
    }

    private final Code code;

    public BudgetException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public BudgetException(Code code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public Code code() {
        return code;
    }
}

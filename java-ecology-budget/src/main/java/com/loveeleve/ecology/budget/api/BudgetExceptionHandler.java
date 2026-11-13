package com.loveeleve.ecology.budget.api;

import com.loveeleve.ecology.budget.exception.BudgetException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 统一错误语义：
 * <ul>
 *     <li>预算不足/租约耗尽：409 或 422，调用方不应重试同类请求；</li>
 *     <li>幂等/状态冲突：409，调用方应查询最终状态；</li>
 *     <li>参数错误：400；</li>
 * </ul>
 */
@RestControllerAdvice
public class BudgetExceptionHandler {

    @ExceptionHandler(BudgetException.class)
    public ResponseEntity<Map<String, Object>> handleBudget(BudgetException ex) {
        HttpStatus status = switch (ex.code()) {
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case INSUFFICIENT_BUDGET, LEASE_EXHAUSTED -> HttpStatus.CONFLICT;
            case IDEMPOTENCY_CONFLICT, FLOW_STATE_CONFLICT, LEASE_STATE_CONFLICT -> HttpStatus.CONFLICT;
            case FLOW_NOT_FOUND, LEASE_NOT_FOUND, BUDGET_NOT_FOUND -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(Map.of(
                "code", ex.code().name(),
                "message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("invalid request");
        return ResponseEntity.badRequest().body(Map.of(
                "code", "INVALID_ARGUMENT",
                "message", message));
    }
}

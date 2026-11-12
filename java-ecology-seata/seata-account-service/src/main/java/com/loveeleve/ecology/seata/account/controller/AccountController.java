package com.loveeleve.ecology.seata.account.controller;

import com.loveeleve.ecology.seata.account.service.AccountService;
import com.loveeleve.ecology.seata.account.service.ConcurrentRequestException;
import com.loveeleve.ecology.seata.account.service.IdempotencyConflictException;
import com.loveeleve.ecology.seata.account.service.InsufficientBalanceException;
import com.loveeleve.ecology.seata.api.AccountDebitRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/debit")
    public void debit(@RequestBody AccountDebitRequest request) {
        accountService.debit(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("code", "INVALID_REQUEST",
                "message", ex.getMessage()));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Map<String, String>> insufficient(InsufficientBalanceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "INSUFFICIENT_BALANCE",
                "message", ex.getMessage()));
    }

    @ExceptionHandler(ConcurrentRequestException.class)
    public ResponseEntity<Map<String, String>> concurrent(ConcurrentRequestException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "CONCURRENT_REQUEST",
                "message", ex.getMessage()));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<Map<String, String>> idempotency(IdempotencyConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "IDEMPOTENCY_CONFLICT",
                "message", ex.getMessage()));
    }
}

package com.loveeleve.ecology.seata.storage.controller;

import com.loveeleve.ecology.seata.api.StorageDeductRequest;
import com.loveeleve.ecology.seata.storage.service.ConcurrentStorageRequestException;
import com.loveeleve.ecology.seata.storage.service.IdempotencyConflictException;
import com.loveeleve.ecology.seata.storage.service.InsufficientStockException;
import com.loveeleve.ecology.seata.storage.service.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/storages")
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/deduct")
    public void deduct(@RequestBody StorageDeductRequest request) {
        storageService.deduct(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("code", "INVALID_REQUEST",
                "message", ex.getMessage()));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, String>> insufficient(InsufficientStockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "INSUFFICIENT_STOCK",
                "message", ex.getMessage()));
    }

    @ExceptionHandler(ConcurrentStorageRequestException.class)
    public ResponseEntity<Map<String, String>> concurrent(ConcurrentStorageRequestException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "CONCURRENT_REQUEST",
                "message", ex.getMessage()));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<Map<String, String>> idempotency(IdempotencyConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "IDEMPOTENCY_CONFLICT",
                "message", ex.getMessage()));
    }
}

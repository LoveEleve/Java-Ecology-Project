package com.loveeleve.ecology.seata.storage.service;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }
}

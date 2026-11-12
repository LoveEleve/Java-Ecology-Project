package com.loveeleve.ecology.seata.storage.service;

public class ConcurrentStorageRequestException extends RuntimeException {

    public ConcurrentStorageRequestException(String message) {
        super(message);
    }
}

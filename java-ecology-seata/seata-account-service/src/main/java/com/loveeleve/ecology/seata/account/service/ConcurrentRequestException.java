package com.loveeleve.ecology.seata.account.service;

public class ConcurrentRequestException extends RuntimeException {

    public ConcurrentRequestException(String message) {
        super(message);
    }
}

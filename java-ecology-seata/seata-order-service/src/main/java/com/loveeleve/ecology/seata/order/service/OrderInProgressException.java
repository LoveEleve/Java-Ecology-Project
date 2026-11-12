package com.loveeleve.ecology.seata.order.service;

public class OrderInProgressException extends RuntimeException {

    public OrderInProgressException(String message) {
        super(message);
    }
}

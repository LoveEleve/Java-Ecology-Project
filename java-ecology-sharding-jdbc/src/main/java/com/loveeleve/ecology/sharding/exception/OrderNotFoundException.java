package com.loveeleve.ecology.sharding.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(long orderId) {
        super("order " + orderId + " not found");
    }
}

package com.loveeleve.ecology.redis.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(long id) {
        super("product " + id + " not found");
    }
}

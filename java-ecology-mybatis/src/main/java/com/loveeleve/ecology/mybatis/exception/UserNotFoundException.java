package com.loveeleve.ecology.mybatis.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(long id) {
        super("user " + id + " not found");
    }
}

package com.loveeleve.ecology.mybatisplus.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(long id) {
        super("user " + id + " not found");
    }
}

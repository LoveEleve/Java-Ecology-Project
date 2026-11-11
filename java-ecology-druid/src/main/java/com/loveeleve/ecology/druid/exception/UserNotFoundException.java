package com.loveeleve.ecology.druid.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(long id) {
        super("user " + id + " not found");
    }
}

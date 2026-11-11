package com.loveeleve.ecology.mybatisplus.exception;

public class OptimisticLockException extends RuntimeException {

    public OptimisticLockException(long id) {
        super("user " + id + " was modified by another request");
    }
}

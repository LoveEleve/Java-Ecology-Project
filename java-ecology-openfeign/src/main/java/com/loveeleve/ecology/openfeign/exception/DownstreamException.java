package com.loveeleve.ecology.openfeign.exception;

public class DownstreamException extends RuntimeException {

    private final int status;

    public DownstreamException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}

package com.loveeleve.ecology.xxljob.model;

public record JobResult(boolean success, int processed, String message) {

    public static JobResult success(int processed, String message) {
        return new JobResult(true, processed, message);
    }

    public static JobResult failure(String message) {
        return new JobResult(false, 0, message);
    }
}

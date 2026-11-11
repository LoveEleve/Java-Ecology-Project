package com.loveeleve.ecology.web.common;

import org.slf4j.MDC;

import java.util.UUID;

public final class RequestIds {

    public static final String MDC_KEY = "requestId";

    private RequestIds() {
    }

    public static String current() {
        String value = MDC.get(MDC_KEY);
        return value == null ? "" : value;
    }

    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}

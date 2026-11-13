package com.loveeleve.ecology.jraft.model;

public record KvCommand(String op, String key, String value) {

    public static KvCommand put(String key, String value) {
        return new KvCommand("PUT", key, value);
    }

    public static KvCommand get(String key) {
        return new KvCommand("GET", key, null);
    }
}

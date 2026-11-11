package com.loveeleve.ecology.multids.config;

public final class DataSourceContextHolder {

    private static final ThreadLocal<DataSourceKey> HOLDER = new ThreadLocal<>();

    private DataSourceContextHolder() {
    }

    public static void set(DataSourceKey key) {
        HOLDER.set(key);
    }

    public static DataSourceKey get() {
        return HOLDER.get();
    }

    public static DataSourceKey getOrDefault() {
        DataSourceKey key = HOLDER.get();
        return key == null ? DataSourceKey.MASTER : key;
    }

    public static void clear() {
        HOLDER.remove();
    }
}

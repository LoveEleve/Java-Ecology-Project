package com.loveeleve.ecology.grpc.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.grpc.client")
public class GrpcClientProperties {

    private String host = "127.0.0.1";
    private int port = 9090;
    private long deadlineMillis = 3000;
    private int maxRetryAttempts = 3;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getDeadlineMillis() {
        return deadlineMillis;
    }

    public void setDeadlineMillis(long deadlineMillis) {
        this.deadlineMillis = deadlineMillis;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }
}

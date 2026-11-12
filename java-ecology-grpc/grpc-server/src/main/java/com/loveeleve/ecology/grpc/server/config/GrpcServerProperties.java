package com.loveeleve.ecology.grpc.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.grpc.server")
public class GrpcServerProperties {

    private boolean enabled = true;
    private int port = 9090;
    private int maxInboundMessageSize = 4 * 1024 * 1024;
    private int keepAliveSeconds = 30;
    private int permitKeepAliveSeconds = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMaxInboundMessageSize() {
        return maxInboundMessageSize;
    }

    public void setMaxInboundMessageSize(int maxInboundMessageSize) {
        this.maxInboundMessageSize = maxInboundMessageSize;
    }

    public int getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    public void setKeepAliveSeconds(int keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }

    public int getPermitKeepAliveSeconds() {
        return permitKeepAliveSeconds;
    }

    public void setPermitKeepAliveSeconds(int permitKeepAliveSeconds) {
        this.permitKeepAliveSeconds = permitKeepAliveSeconds;
    }
}

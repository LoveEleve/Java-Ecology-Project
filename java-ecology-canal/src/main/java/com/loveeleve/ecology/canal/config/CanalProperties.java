package com.loveeleve.ecology.canal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.canal")
public class CanalProperties {

    private boolean enabled = false;
    private String destination = "example";
    private String host = "127.0.0.1";
    private int port = 11111;
    private String username = "canal";
    private String password = "canal";
    private boolean clusterEnabled = false;
    private String zookeeperServers = "127.0.0.1:2181";
    private String filter = ".*\\..*";
    private int batchSize = 1000;
    private long getTimeoutMillis = 1000;
    private long reconnectBackoffMillis = 3000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isClusterEnabled() {
        return clusterEnabled;
    }

    public void setClusterEnabled(boolean clusterEnabled) {
        this.clusterEnabled = clusterEnabled;
    }

    public String getZookeeperServers() {
        return zookeeperServers;
    }

    public void setZookeeperServers(String zookeeperServers) {
        this.zookeeperServers = zookeeperServers;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getGetTimeoutMillis() {
        return getTimeoutMillis;
    }

    public void setGetTimeoutMillis(long getTimeoutMillis) {
        this.getTimeoutMillis = getTimeoutMillis;
    }

    public long getReconnectBackoffMillis() {
        return reconnectBackoffMillis;
    }

    public void setReconnectBackoffMillis(long reconnectBackoffMillis) {
        this.reconnectBackoffMillis = reconnectBackoffMillis;
    }
}

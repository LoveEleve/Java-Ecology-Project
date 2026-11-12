package com.loveeleve.ecology.nacos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ecology.nacos")
public class NacosDemoProperties {

    private String message = "local-default";
    private String group = "ECO_ECOLOGY";
    private boolean publishEnabled = false;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean isPublishEnabled() {
        return publishEnabled;
    }

    public void setPublishEnabled(boolean publishEnabled) {
        this.publishEnabled = publishEnabled;
    }
}

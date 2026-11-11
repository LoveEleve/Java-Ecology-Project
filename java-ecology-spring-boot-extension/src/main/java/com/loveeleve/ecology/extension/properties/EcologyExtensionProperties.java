package com.loveeleve.ecology.extension.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ecology.extension")
public class EcologyExtensionProperties {

    private boolean enabled = true;
    private String message = "Spring Boot extension points are active";
    private boolean bindListenerEnabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isBindListenerEnabled() {
        return bindListenerEnabled;
    }

    public void setBindListenerEnabled(boolean bindListenerEnabled) {
        this.bindListenerEnabled = bindListenerEnabled;
    }
}

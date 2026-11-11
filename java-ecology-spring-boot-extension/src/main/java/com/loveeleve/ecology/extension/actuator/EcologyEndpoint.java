package com.loveeleve.ecology.extension.actuator;

import com.loveeleve.ecology.extension.properties.EcologyExtensionProperties;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.LinkedHashMap;
import java.util.Map;

@Endpoint(id = "ecology")
public class EcologyEndpoint {

    private final EcologyExtensionProperties properties;

    public EcologyEndpoint(EcologyExtensionProperties properties) {
        this.properties = properties;
    }

    @ReadOperation
    public Map<String, Object> info() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", properties.isEnabled());
        result.put("message", properties.getMessage());
        result.put("bindListenerEnabled", properties.isBindListenerEnabled());
        return result;
    }
}

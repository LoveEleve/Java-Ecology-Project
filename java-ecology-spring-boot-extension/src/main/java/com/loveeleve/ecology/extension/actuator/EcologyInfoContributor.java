package com.loveeleve.ecology.extension.actuator;

import com.loveeleve.ecology.extension.properties.EcologyExtensionProperties;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

public class EcologyInfoContributor implements InfoContributor {

    private final EcologyExtensionProperties properties;

    public EcologyInfoContributor(EcologyExtensionProperties properties) {
        this.properties = properties;
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("ecology", properties.getMessage());
    }
}

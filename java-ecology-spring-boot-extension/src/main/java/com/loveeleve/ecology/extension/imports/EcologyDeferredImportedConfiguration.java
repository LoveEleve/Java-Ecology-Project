package com.loveeleve.ecology.extension.imports;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class EcologyDeferredImportedConfiguration {

    @Bean
    public EcologyDeferredImportedMarker ecologyDeferredImportedMarker() {
        return new EcologyDeferredImportedMarker();
    }
}

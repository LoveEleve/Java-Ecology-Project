package com.loveeleve.ecology.web.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            factory.addConnectorCustomizers(connector -> {
                connector.setURIEncoding(StandardCharsets.UTF_8.name());
                connector.setMaxPostSize(2 * 1024 * 1024);
            });
            factory.addContextCustomizers(context -> {
                context.setUseHttpOnly(true);
                context.setSessionCookieName("ECOLOGY_SESSION");
                context.setSessionTimeout(30);
            });
        };
    }
}

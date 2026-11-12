package com.loveeleve.ecology.websocket.config;

import com.loveeleve.ecology.websocket.handler.ChatWebSocketHandler;
import com.loveeleve.ecology.websocket.interceptor.WebSocketAuthInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
@EnableConfigurationProperties(WebSocketProperties.class)
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler handler;
    private final WebSocketAuthInterceptor authInterceptor;
    private final WebSocketProperties properties;

    public WebSocketConfig(ChatWebSocketHandler handler, WebSocketAuthInterceptor authInterceptor,
                           WebSocketProperties properties) {
        this.handler = handler;
        this.authInterceptor = authInterceptor;
        this.properties = properties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, properties.getPath())
                .addInterceptors(authInterceptor)
                .setAllowedOrigins(properties.getAllowedOrigins().toArray(String[]::new));
    }

    @Bean
    public ServletServerContainerFactoryBean webSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(properties.getMaxTextMessageBytes());
        container.setMaxBinaryMessageBufferSize(properties.getMaxTextMessageBytes());
        container.setMaxSessionIdleTimeout(properties.getIdleTimeoutMillis());
        return container;
    }
}

package com.loveeleve.ecology.redis.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration(proxyBeanMethods = false)
public class RedisPubSubConfig {

    public static final String CHANNEL = "ecology:redis:pubsub:demo";
    private static final Logger log = LoggerFactory.getLogger(RedisPubSubConfig.class);

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) ->
                log.info("redis pubsub [{}]: {}", CHANNEL,
                        new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8)),
                new ChannelTopic(CHANNEL));
        return container;
    }
}

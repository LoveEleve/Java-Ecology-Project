package com.loveeleve.ecology.kafka.config;

import com.loveeleve.ecology.kafka.dto.OrderEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
public class KafkaConfig {

    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name(KafkaTopics.ORDERS)
                .partitions(3)
                .replicas(1)
                .config("min.insync.replicas", "1")
                .config("retention.ms", "604800000")
                .build();
    }

    @Bean
    public NewTopic ordersBatchTopic() {
        return TopicBuilder.name(KafkaTopics.ORDERS_BATCH)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }

    @Bean
    public NewTopic ordersDltTopic() {
        return TopicBuilder.name(KafkaTopics.ORDERS_DLT)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "1209600000")
                .build();
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(KafkaTopics.ORDERS_DLT, record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(200, 2));
    }
}

package com.loveeleve.ecology.kafka;

import com.loveeleve.ecology.kafka.config.KafkaTopics;
import com.loveeleve.ecology.kafka.dto.OrderEvent;
import com.loveeleve.ecology.kafka.producer.OrderProducer;
import com.loveeleve.ecology.kafka.service.ProcessedOrderStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(
        partitions = 3,
        topics = {KafkaTopics.ORDERS, KafkaTopics.ORDERS_DLT, KafkaTopics.ORDERS_BATCH},
        brokerProperties = {"auto.create.topics.enable=false"}
)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.group-id=test-ecology-kafka",
        "spring.kafka.listener.ack-mode=manual_immediate",
        "app.kafka.consumer-group=test-ecology-kafka",
        "app.kafka.dlt-group=test-ecology-kafka-dlt",
        "app.kafka.batch-group=test-ecology-kafka-batch"
})
class KafkaIntegrationTests {

    @Autowired
    private OrderProducer producer;

    @Autowired
    private ProcessedOrderStore store;

    @Test
    void consumesAndDeduplicatesEvents() {
        OrderEvent event = new OrderEvent("order-1", "user-1", 100, false);
        producer.send(event);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(store.contains("order-1")).isTrue();
            assertThat(store.processedCount()).isEqualTo(1);
        });

        producer.send(event);
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(store.processedCount()).isEqualTo(1));
    }

    @Test
    void batchListenerConsumesMultipleEvents() {
        java.util.List<OrderEvent> events = java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> new OrderEvent("batch-" + i, "user-1", 100, false))
                .toList();
        producer.sendBatch(events);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            for (OrderEvent event : events) {
                assertThat(store.contains(event.getOrderId())).isTrue();
            }
        });
    }

    @Test
    void failedEventGoesToDlt() {
        producer.send(new OrderEvent("fail-1", "user-1", 100, true));
        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(store.failedCount()).isEqualTo(1));
    }
}

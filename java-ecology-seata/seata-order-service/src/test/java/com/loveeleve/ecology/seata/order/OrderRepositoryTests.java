package com.loveeleve.ecology.seata.order;

import com.loveeleve.ecology.seata.api.CreateOrderRequest;
import com.loveeleve.ecology.seata.order.repository.OrderRecord;
import com.loveeleve.ecology.seata.order.repository.OrderRepository;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRepositoryTests {

    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:seata-order-repo;MODE=MySQL;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP ALL OBJECTS");
        executeScript(jdbcTemplate, "src/main/resources/db/schema-h2.sql");
        orderRepository = new OrderRepository(jdbcTemplate);
    }

    @Test
    void insertGeneratesIdAndRoundTripsPayload() {
        CreateOrderRequest request = new CreateOrderRequest(
                "repo-1", 1L, 2L, new BigDecimal("100.00"), 3);

        long orderId = orderRepository.insert(request);

        assertThat(orderId).isPositive();
        OrderRecord record = orderRepository.findByRequestId("repo-1").orElseThrow();
        assertThat(record.orderId()).isEqualTo(orderId);
        assertThat(record.status()).isEqualTo("CREATED");
        assertThat(record.userId()).isEqualTo(1L);
        assertThat(record.productId()).isEqualTo(2L);
        assertThat(record.amount()).isEqualByComparingTo("100.00");
        assertThat(record.quantity()).isEqualTo(3);

        orderRepository.updateStatus(orderId, "CONFIRMED");
        assertThat(orderRepository.findByRequestId("repo-1").orElseThrow().status())
                .isEqualTo("CONFIRMED");
    }

    private void executeScript(JdbcTemplate jdbcTemplate, String path) throws Exception {
        String schema = Files.readString(Path.of(path), StandardCharsets.UTF_8);
        for (String sql : schema.split(";")) {
            if (!sql.isBlank()) {
                jdbcTemplate.execute(sql);
            }
        }
    }
}

package com.loveeleve.ecology.sharding;

import com.loveeleve.ecology.sharding.model.Order;
import com.loveeleve.ecology.sharding.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("shadow")
class ShadowShardingTests {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    @Qualifier("shadowProdJdbcTemplate")
    private JdbcTemplate prod;

    @Autowired
    @Qualifier("shadowCopyJdbcTemplate")
    private JdbcTemplate shadow;

    @BeforeEach
    void clean() {
        prod.update("DELETE FROM t_order");
        shadow.update("DELETE FROM t_order");
    }

    @Test
    void matchingUserIsRoutedToShadowDatabase() {
        orderRepository.insert(new Order(1, 1, 100, "SHADOW"));
        orderRepository.insert(new Order(2, 2, 200, "PROD"));

        assertThat(count(shadow)).isEqualTo(1);
        assertThat(count(prod)).isEqualTo(1);
        assertThat(orderRepository.find(1, 1)).get()
                .extracting(Order::status).isEqualTo("SHADOW");
        assertThat(orderRepository.find(2, 2)).get()
                .extracting(Order::status).isEqualTo("PROD");
    }

    private int count(JdbcTemplate jdbcTemplate) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_order", Integer.class);
        return count == null ? 0 : count;
    }
}

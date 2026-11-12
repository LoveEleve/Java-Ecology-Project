package com.loveeleve.ecology.sharding;

import com.loveeleve.ecology.sharding.model.Order;
import com.loveeleve.ecology.sharding.repository.OrderRepository;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("readwrite")
class ReadWriteShardingTests {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    @Qualifier("ds0MasterJdbcTemplate")
    private JdbcTemplate ds0Master;

    @Autowired
    @Qualifier("ds0SlaveJdbcTemplate")
    private JdbcTemplate ds0Slave;

    @Autowired
    @Qualifier("ds1MasterJdbcTemplate")
    private JdbcTemplate ds1Master;

    @Autowired
    @Qualifier("ds1SlaveJdbcTemplate")
    private JdbcTemplate ds1Slave;

    @BeforeEach
    void clean() {
        for (JdbcTemplate jdbcTemplate : new JdbcTemplate[]{ds0Master, ds0Slave, ds1Master, ds1Slave}) {
            jdbcTemplate.update("DELETE FROM t_order_0");
            jdbcTemplate.update("DELETE FROM t_order_1");
        }
    }

    @Test
    void writesGoToMasterAndReadsGoToSlave() {
        ds0Slave.update("""
                INSERT INTO t_order_0 (order_id, user_id, amount, status)
                VALUES (2, 2, 200, 'SLAVE-ONLY')
                """);

        orderRepository.insert(new Order(1, 1, 100, "MASTER-ONLY"));

        assertThat(count(ds1Master, "t_order_1")).isEqualTo(1);
        assertThat(count(ds1Slave, "t_order_1")).isZero();

        assertThat(orderRepository.find(2, 2)).get()
                .extracting(Order::status).isEqualTo("SLAVE-ONLY");
        assertThat(orderRepository.find(1, 1)).isEmpty();

        try (HintManager hintManager = HintManager.getInstance()) {
            hintManager.setWriteRouteOnly();
            assertThat(orderRepository.find(1, 1)).get()
                    .extracting(Order::status).isEqualTo("MASTER-ONLY");
        }
    }

    private int count(JdbcTemplate jdbcTemplate, String table) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return count == null ? 0 : count;
    }
}

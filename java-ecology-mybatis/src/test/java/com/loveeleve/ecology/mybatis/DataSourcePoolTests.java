package com.loveeleve.ecology.mybatis;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DataSourcePoolTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void hikariPoolUsesProductionParameters() throws Exception {
        HikariDataSource hikari = dataSource.unwrap(HikariDataSource.class);

        assertThat(hikari.getPoolName()).isEqualTo("ecology-mybatis-pool");
        assertThat(hikari.getMaximumPoolSize()).isEqualTo(20);
        assertThat(hikari.getMinimumIdle()).isEqualTo(5);
        assertThat(hikari.getConnectionTimeout()).isEqualTo(3000);
        assertThat(hikari.getValidationTimeout()).isEqualTo(1000);
        assertThat(hikari.getIdleTimeout()).isEqualTo(600000);
        assertThat(hikari.getMaxLifetime()).isEqualTo(1800000);
        assertThat(hikari.getKeepaliveTime()).isEqualTo(60000);
        assertThat(hikari.getLeakDetectionThreshold()).isEqualTo(20000);
        assertThat(hikari.getTransactionIsolation()).isEqualTo("TRANSACTION_READ_COMMITTED");
    }

    @Test
    void exposesHikariMetricsWithPoolTag() {
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        assertThat(meterRegistry.find("hikaricp.connections.max")
                .tag("pool", "ecology-mybatis-pool")
                .gauge()).isNotNull();
    }
}

package com.loveeleve.ecology.druid;

import com.alibaba.druid.pool.DruidDataSource;
import com.loveeleve.ecology.druid.dto.PoolStats;
import com.loveeleve.ecology.druid.service.PoolStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DruidPoolTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PoolStatsService statsService;

    @Test
    void druidPoolUsesProductionParameters() throws Exception {
        DruidDataSource druid = dataSource.unwrap(DruidDataSource.class);

        assertThat(druid.getName()).isEqualTo("ecology-druid-pool");
        assertThat(druid.getInitialSize()).isEqualTo(5);
        assertThat(druid.getMinIdle()).isEqualTo(5);
        assertThat(druid.getMaxActive()).isEqualTo(20);
        assertThat(druid.getMaxWait()).isEqualTo(3000);
        assertThat(druid.getValidationQuery()).isEqualTo("SELECT 1");
        assertThat(druid.isTestWhileIdle()).isTrue();
        assertThat(druid.isTestOnBorrow()).isFalse();
        assertThat(druid.isTestOnReturn()).isFalse();
        assertThat(druid.isKeepAlive()).isTrue();
        assertThat(druid.getKeepAliveBetweenTimeMillis()).isEqualTo(120000);
        assertThat(druid.getPhyTimeoutMillis()).isEqualTo(1800000);
        assertThat(druid.isRemoveAbandoned()).isFalse();
    }

    @Test
    void poolStatsReflectsLiveDruidState() {
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        PoolStats stats = statsService.snapshot();

        assertThat(stats.name()).isEqualTo("ecology-druid-pool");
        assertThat(stats.maxActive()).isEqualTo(20);
        assertThat(stats.minIdle()).isEqualTo(5);
        assertThat(stats.activeCount()).isGreaterThanOrEqualTo(0);
        assertThat(stats.poolingCount()).isGreaterThanOrEqualTo(0);
    }
}

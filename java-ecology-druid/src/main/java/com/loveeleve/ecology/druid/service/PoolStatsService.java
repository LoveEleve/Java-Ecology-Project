package com.loveeleve.ecology.druid.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.loveeleve.ecology.druid.dto.PoolStats;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;

@Service
public class PoolStatsService {

    private final DataSource dataSource;

    public PoolStatsService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public PoolStats snapshot() {
        DruidDataSource druid;
        try {
            druid = dataSource.unwrap(DruidDataSource.class);
        } catch (SQLException ex) {
            throw new IllegalStateException("data source is not a DruidDataSource", ex);
        }
        return new PoolStats(
                druid.getName(),
                druid.getActiveCount(),
                druid.getPoolingCount(),
                druid.getMaxActive(),
                druid.getMinIdle(),
                druid.getWaitThreadCount(),
                druid.getCreateCount(),
                druid.getDestroyCount(),
                druid.getConnectCount(),
                druid.getErrorCount(),
                druid.getDiscardCount()
        );
    }
}

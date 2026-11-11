package com.loveeleve.ecology.multids;

import com.loveeleve.ecology.multids.config.DataSourceContextHolder;
import com.loveeleve.ecology.multids.config.DataSourceKey;
import com.loveeleve.ecology.multids.model.Account;
import com.loveeleve.ecology.multids.service.DynamicAccountService;
import com.loveeleve.ecology.multids.service.StaticAccountService;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MultiDataSourceTests {

    @Autowired
    private StaticAccountService staticService;

    @Autowired
    private DynamicAccountService dynamicService;

    @Autowired
    @Qualifier("masterDataSource")
    private HikariDataSource masterDataSource;

    @Autowired
    @Qualifier("reportDataSource")
    private HikariDataSource reportDataSource;

    @AfterEach
    void clearContext() {
        DataSourceContextHolder.clear();
    }

    @Test
    void eachDataSourceKeepsItsOwnPoolConfiguration() {
        assertThat(masterDataSource.getPoolName()).isEqualTo("multi-master-pool");
        assertThat(reportDataSource.getPoolName()).isEqualTo("multi-report-pool");
        assertThat(masterDataSource.getMaximumPoolSize()).isEqualTo(10);
        assertThat(reportDataSource.getMaximumPoolSize()).isEqualTo(10);
        assertThat(masterDataSource.getConnectionTimeout()).isEqualTo(3000);
        assertThat(masterDataSource.getLeakDetectionThreshold()).isEqualTo(20000);
    }

    @Test
    void staticDataSourcesAreCompletelyIsolated() {
        assertThat(staticService.masterAccounts())
                .extracting(Account::owner)
                .containsExactly("master-alice", "master-bob");
        assertThat(staticService.reportAccounts())
                .extracting(Account::owner)
                .containsExactly("report-carol", "report-dave", "report-eve");
    }

    @Test
    void annotationRoutingSelectsTargetDataSource() {
        assertThat(dynamicService.masterAccountsByAnnotation())
                .extracting(Account::owner)
                .containsExactly("master-alice", "master-bob");
        assertThat(dynamicService.reportAccountsByAnnotation())
                .extracting(Account::owner)
                .containsExactly("report-carol", "report-dave", "report-eve");
    }

    @Test
    void programmaticRoutingClearsThreadLocalAfterCall() {
        assertThat(dynamicService.accountsByKey(DataSourceKey.REPORT))
                .extracting(Account::owner)
                .contains("report-carol");
        assertThat(DataSourceContextHolder.get()).isNull();
    }
}

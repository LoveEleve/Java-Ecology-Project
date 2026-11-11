package com.loveeleve.ecology.multids.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class MultiDataSourceConfig {

    @Bean
    @ConfigurationProperties("app.datasource.master")
    public DataSourceProperties masterDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("app.datasource.report")
    public DataSourceProperties reportDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "masterDataSource")
    @ConfigurationProperties("app.datasource.master.hikari")
    public HikariDataSource masterDataSource(
            @Qualifier("masterDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "reportDataSource")
    @ConfigurationProperties("app.datasource.report.hikari")
    public HikariDataSource reportDataSource(
            @Qualifier("reportDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "masterJdbcTemplate")
    public JdbcTemplate masterJdbcTemplate(@Qualifier("masterDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "reportJdbcTemplate")
    public JdbcTemplate reportJdbcTemplate(@Qualifier("reportDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @Primary
    public DataSource routingDataSource(@Qualifier("masterDataSource") DataSource master,
                                        @Qualifier("reportDataSource") DataSource report) {
        Map<Object, Object> targets = new HashMap<>();
        targets.put(DataSourceKey.MASTER, master);
        targets.put(DataSourceKey.REPORT, report);

        RoutingDataSource routing = new RoutingDataSource();
        routing.setTargetDataSources(targets);
        routing.setDefaultTargetDataSource(master);
        routing.afterPropertiesSet();
        return routing;
    }

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("routingDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public CommandLineRunner initializeDatabases(@Qualifier("masterDataSource") DataSource master,
                                                 @Qualifier("reportDataSource") DataSource report) {
        return args -> {
            new ResourceDatabasePopulator(
                    new ClassPathResource("db/master-schema.sql"),
                    new ClassPathResource("db/master-data.sql")).execute(master);
            new ResourceDatabasePopulator(
                    new ClassPathResource("db/report-schema.sql"),
                    new ClassPathResource("db/report-data.sql")).execute(report);
        };
    }
}

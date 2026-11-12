package com.loveeleve.ecology.sharding.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@Profile("!readwrite & !shadow")
public class ShardingDataSourceConfig {

    @Bean
    @ConfigurationProperties("app.sharding.datasources.ds0")
    public DataSourceProperties ds0DataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("app.sharding.datasources.ds1")
    public DataSourceProperties ds1DataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "ds0DataSource")
    @ConfigurationProperties("app.sharding.datasources.ds0.hikari")
    public HikariDataSource ds0DataSource(
            @Qualifier("ds0DataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "ds1DataSource")
    @ConfigurationProperties("app.sharding.datasources.ds1.hikari")
    public HikariDataSource ds1DataSource(
            @Qualifier("ds1DataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "shardingDataSource")
    @Primary
    public DataSource shardingDataSource(@Qualifier("ds0DataSource") DataSource ds0,
                                         @Qualifier("ds1DataSource") DataSource ds1,
                                         @Value("${app.sharding.init-schema:true}") boolean initSchema)
            throws Exception {
        if (initSchema) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                    new ClassPathResource("db/schema-h2.sql"));
            populator.execute(ds0);
            populator.execute(ds1);
        }
        Map<String, DataSource> dataSources = new LinkedHashMap<>();
        dataSources.put("ds_0", ds0);
        dataSources.put("ds_1", ds1);
        byte[] yaml = new ClassPathResource("sharding.yaml").getInputStream().readAllBytes();
        return YamlShardingSphereDataSourceFactory.createDataSource(dataSources, yaml);
    }

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("shardingDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "ds0JdbcTemplate")
    public JdbcTemplate ds0JdbcTemplate(@Qualifier("ds0DataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "ds1JdbcTemplate")
    public JdbcTemplate ds1JdbcTemplate(@Qualifier("ds1DataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}

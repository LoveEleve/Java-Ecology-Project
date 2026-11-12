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
@Profile("readwrite")
public class ReadWriteShardingConfig {

    @Bean
    @ConfigurationProperties("app.readwrite.datasources.ds0master")
    public DataSourceProperties ds0MasterProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("app.readwrite.datasources.ds0slave")
    public DataSourceProperties ds0SlaveProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("app.readwrite.datasources.ds1master")
    public DataSourceProperties ds1MasterProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("app.readwrite.datasources.ds1slave")
    public DataSourceProperties ds1SlaveProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "ds0Master")
    @ConfigurationProperties("app.readwrite.datasources.ds0master.hikari")
    public HikariDataSource ds0Master(@Qualifier("ds0MasterProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "ds0Slave")
    @ConfigurationProperties("app.readwrite.datasources.ds0slave.hikari")
    public HikariDataSource ds0Slave(@Qualifier("ds0SlaveProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "ds1Master")
    @ConfigurationProperties("app.readwrite.datasources.ds1master.hikari")
    public HikariDataSource ds1Master(@Qualifier("ds1MasterProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "ds1Slave")
    @ConfigurationProperties("app.readwrite.datasources.ds1slave.hikari")
    public HikariDataSource ds1Slave(@Qualifier("ds1SlaveProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "shardingDataSource")
    @Primary
    public DataSource shardingDataSource(@Qualifier("ds0Master") DataSource ds0Master,
                                         @Qualifier("ds0Slave") DataSource ds0Slave,
                                         @Qualifier("ds1Master") DataSource ds1Master,
                                         @Qualifier("ds1Slave") DataSource ds1Slave,
                                         @Value("${app.readwrite.init-schema:true}") boolean initSchema)
            throws Exception {
        if (initSchema) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                    new ClassPathResource("db/schema-h2.sql"));
            populator.execute(ds0Master);
            populator.execute(ds0Slave);
            populator.execute(ds1Master);
            populator.execute(ds1Slave);
        }
        Map<String, DataSource> dataSources = new LinkedHashMap<>();
        dataSources.put("ds_0_master", ds0Master);
        dataSources.put("ds_0_slave", ds0Slave);
        dataSources.put("ds_1_master", ds1Master);
        dataSources.put("ds_1_slave", ds1Slave);
        byte[] yaml = new ClassPathResource("sharding-readwrite.yaml").getInputStream().readAllBytes();
        return YamlShardingSphereDataSourceFactory.createDataSource(dataSources, yaml);
    }

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("shardingDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "ds0MasterJdbcTemplate")
    public JdbcTemplate ds0MasterJdbcTemplate(@Qualifier("ds0Master") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "ds0SlaveJdbcTemplate")
    public JdbcTemplate ds0SlaveJdbcTemplate(@Qualifier("ds0Slave") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "ds1MasterJdbcTemplate")
    public JdbcTemplate ds1MasterJdbcTemplate(@Qualifier("ds1Master") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "ds1SlaveJdbcTemplate")
    public JdbcTemplate ds1SlaveJdbcTemplate(@Qualifier("ds1Slave") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}

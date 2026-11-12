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
@Profile("shadow")
public class ShadowShardingConfig {

    @Bean
    @ConfigurationProperties("app.shadow.datasources.prod")
    public DataSourceProperties shadowProdProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("app.shadow.datasources.shadow")
    public DataSourceProperties shadowCopyProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "shadowProdDataSource")
    @ConfigurationProperties("app.shadow.datasources.prod.hikari")
    public HikariDataSource shadowProdDataSource(
            @Qualifier("shadowProdProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "shadowCopyDataSource")
    @ConfigurationProperties("app.shadow.datasources.shadow.hikari")
    public HikariDataSource shadowCopyDataSource(
            @Qualifier("shadowCopyProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "shardingDataSource")
    @Primary
    public DataSource shardingDataSource(@Qualifier("shadowProdDataSource") DataSource prod,
                                         @Qualifier("shadowCopyDataSource") DataSource shadow,
                                         @Value("${app.shadow.init-schema:true}") boolean initSchema)
            throws Exception {
        if (initSchema) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                    new ClassPathResource("db/schema-shadow.sql"));
            populator.execute(prod);
            populator.execute(shadow);
        }
        Map<String, DataSource> dataSources = new LinkedHashMap<>();
        dataSources.put("ds_prod", prod);
        dataSources.put("ds_shadow", shadow);
        byte[] yaml = new ClassPathResource("sharding-shadow.yaml").getInputStream().readAllBytes();
        return YamlShardingSphereDataSourceFactory.createDataSource(dataSources, yaml);
    }

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("shardingDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "shadowProdJdbcTemplate")
    public JdbcTemplate shadowProdJdbcTemplate(@Qualifier("shadowProdDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "shadowCopyJdbcTemplate")
    public JdbcTemplate shadowCopyJdbcTemplate(@Qualifier("shadowCopyDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}

package com.agent.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Dual-datasource configuration.
 *
 * - primary:  app_user (read-write), used by the main application and Flyway.
 * - readonly: app_readonly (SELECT only), used by QueryExecutionService for SQL execution.
 *
 * Separation ensures that even if the AI-generated SQL is somehow executed
 * without proper validation, the readonly account cannot mutate data.
 *
 * NOTE: The readonly datasource is only created when app.datasource.readonly.enabled=true
 * (default false). This allows T01 to run with a simple single-datasource setup.
 */
@Configuration
public class DataSourceConfig {

    /**
     * Readonly datasource — disabled by default.
     * Enable with: app.datasource.readonly.enabled=true in application.yml
     */
    @Bean(name = "readonlyDataSource")
    @ConditionalOnProperty(name = "app.datasource.readonly.enabled", havingValue = "true")
    @ConfigurationProperties(prefix = "spring.datasource.readonly")
    public DataSource readonlyDataSource() {
        return DataSourceBuilder.create().build();
    }
}

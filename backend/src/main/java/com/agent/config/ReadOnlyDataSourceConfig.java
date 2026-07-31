package com.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Read-only JdbcTemplate for SQL query execution.
 *
 * Uses the primary datasource with connection-level readOnly enforcement.
 * When a separate app_readonly MySQL user is configured, it can be swapped in
 * via the datasource.readonly properties (M4+).
 */
@Configuration
public class ReadOnlyDataSourceConfig {

    @Bean(name = "readOnlyJdbcTemplate")
    public JdbcTemplate readOnlyJdbcTemplate(DataSource dataSource) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.setMaxRows(1000);  // Hard row limit
        template.setQueryTimeout(30); // 30-second timeout
        return template;
    }
}

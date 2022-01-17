package io.roach.workload.common.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariDataSource;

import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

@Configuration
public class DataSourceConfig {
    public static final String SQL_TRACE_LOGGER = "io.roach.SQL_TRACE";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Logger traceLogger = LoggerFactory.getLogger(SQL_TRACE_LOGGER);

    @Value("${roach.application-name}")
    private String appName;

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource targetDataSource() {
        HikariDataSource ds = dataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();

        ds.addDataSourceProperty("reWriteBatchedInserts", true); // case sensitive

//        ds.addDataSourceProperty("cachePrepStmts", "true");
//        ds.addDataSourceProperty("prepStmtCacheSize", "250");
//        ds.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
//        ds.addDataSourceProperty("useServerPrepStmts", "true");

        ds.addDataSourceProperty("application_name", appName);
        ds.addDataSourceProperty("ApplicationName", appName);
        return ds;
    }

    @Bean
    @Primary
    public DataSource primaryDataSource() {
        return loggingProxy(targetDataSource());
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(primaryDataSource());
    }

    private DataSource loggingProxy(DataSource dataSource) {
        if (traceLogger.isTraceEnabled()) {
            logger.warn("SQL trace logging enabled, be advised there's a performance impact");
        }
        return ProxyDataSourceBuilder
                .create(dataSource)
                .name("SQL-Trace")
                .asJson()
                .countQuery()
                .logQueryBySlf4j(SLF4JLogLevel.TRACE, SQL_TRACE_LOGGER)
                .multiline()
                .build();
    }
}

package io.roach.workload.events;

import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.util.PropertyPlaceholderHelper;

import io.roach.workload.common.util.ResourceSupport;

public class SchemaSupport {
    private final DataSource dataSource;

    private final JdbcTemplate jdbcTemplate;

    public SchemaSupport(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void dropSchema(int partitions) {
        final PropertyPlaceholderHelper plh = new PropertyPlaceholderHelper("${", "}");
        DatabasePopulatorUtils.execute(connection -> IntStream.rangeClosed(1, partitions)
                .forEach(p -> {
                    String sql = plh
                            .replacePlaceholders(ResourceSupport.resourceAsString("db/events/drop-events.sql"),
                                    placeholderName -> {
                                        if ("partition".equals(placeholderName)) {
                                            return p + "";
                                        }
                                        return "???";
                                    });
                    jdbcTemplate.execute(sql);
                }), dataSource);
    }

    public void createSchema(int partitions) {
        final PropertyPlaceholderHelper plh = new PropertyPlaceholderHelper("${", "}");
        DatabasePopulatorUtils.execute(connection -> IntStream.rangeClosed(1, partitions)
                .forEach(p -> {
                    String sql = plh
                            .replacePlaceholders(ResourceSupport.resourceAsString("db/events/create-events.sql"),
                                    placeholderName -> {
                                        if ("partition".equals(placeholderName)) {
                                            return p + "";
                                        }
                                        return "???";
                                    });
                    jdbcTemplate.execute(sql);
                }), dataSource);
    }
}

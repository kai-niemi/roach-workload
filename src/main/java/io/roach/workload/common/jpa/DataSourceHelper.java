package io.roach.workload.common.jpa;

import java.util.Arrays;

import javax.sql.DataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

public abstract class DataSourceHelper {
    public static String databaseVersion(DataSource dataSource) {
        try {
            return new JdbcTemplate(dataSource).queryForObject("select version()", String.class);
        } catch (DataAccessException e) {
            return "(unknown)";
        }
    }

    public static void executeScripts(DataSource dataSource, String... paths) {
        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
        Arrays.stream(paths).sequential().forEach(p -> {
            databasePopulator.addScript(new ClassPathResource(p));
        });
        databasePopulator.setCommentPrefix("--");
        databasePopulator.setIgnoreFailedDrops(true);
        DatabasePopulatorUtils.execute(databasePopulator, dataSource);
    }
}

package io.roach.workload.query;

import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;

import io.roach.workload.Profiles;
import io.roach.workload.common.AbstractWorkload;
import io.roach.workload.common.util.DurationFormat;

@Profiles.Query
@ShellComponent
public class QueryWorkload extends AbstractWorkload {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    @ShellMethod(value = "Print workload info")
    public void info() {
        printInfo();
    }

    @Override
    public Metadata getMetadata() {
        return new Metadata() {
            @Override
            public String prompt() {
                return "query";
            }

            @Override
            public String name() {
                return "Query";
            }

            @Override
            public String description() {
                return "Adhoc SQL query workload";
            }
        };
    }

    @ShellMethod(value = "Initialize workload")
    public void init(@ShellOption(help = "schema file") String file) {
        console.green("Executing %s..\n", file);
        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
        databasePopulator.addScript(new FileSystemResource(file));
        databasePopulator.setCommentPrefix("--");
        databasePopulator.setIgnoreFailedDrops(true);
        DatabasePopulatorUtils.execute(databasePopulator, dataSource);
    }

    @ShellMethod(value = "Run workload")
    public void run(
            @ShellOption(help = "SQL statement or file (with 'file:' prefix)", defaultValue = "select 1") String sql,
            @ShellOption(help = "number of threads", defaultValue = "-1") int threads,
            @ShellOption(help = "execution duration (expression)", defaultValue = "30m") String duration
    ) throws IOException {
        Duration runtimeDuration = DurationFormat.parseDuration(duration);

        if (threads <= 0) {
            threads = Runtime.getRuntime().availableProcessors();
        }

        String finalSql;
        if (sql.startsWith("file:")) {
            finalSql = FileCopyUtils.copyToString(new FileReader(sql.substring(5)));
        } else {
            finalSql = sql;
        }

        console.blue(">> Starting workload <<\n");
        console.yellow("SQL: %s\n", finalSql);
        console.yellow("Number of threads: %s\n", threads);
        console.yellow("Runtime duration: %s\n", duration);

        IntStream.rangeClosed(1, threads).forEach(value -> {
            boundedExecutor.submit(() -> {
                jdbcTemplate.execute(finalSql, (PreparedStatementCallback<Object>) ps -> null);
            }, "query #" + value, runtimeDuration);
        });
    }
}

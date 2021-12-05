package io.roach.workload.events;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import io.roach.workload.Profiles;
import io.roach.workload.common.AbstractWorkload;
import io.roach.workload.common.util.BoundedExecutor;
import io.roach.workload.common.util.DurationFormat;
import io.roach.workload.common.util.Multiplier;
import io.roach.workload.common.util.RandomData;
import io.roach.workload.events.model.OutboxEvent;

@Profiles.Events
@ShellComponent
public class EventsWorkload extends AbstractWorkload {
    @Autowired
    private BoundedExecutor boundedExecutor;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public String prompt() {
        return "events:$ ";
    }

    @ShellMethod(value = "Initialize events workload")
    public void init(
            @ShellOption(help = "number of tables", defaultValue = "10") int partitions,
            @ShellOption(help = "drop and create schema", defaultValue = "false") boolean drop) {
        SchemaSupport schemaSupport = new SchemaSupport(dataSource);
        if (drop) {
            console.yellow("Dropping %d tables\n", partitions);
            schemaSupport.dropSchema(partitions);
        }
        console.yellow("Creating %d tables\n", partitions);
        schemaSupport.createSchema(partitions);
    }

    @ShellMethod(value = "Run events workload")
    public void run(
            @ShellOption(help = "number of threads per partition", defaultValue = "1") int threads,
            @ShellOption(help = "number of partitions (tables)", defaultValue = "10") int partitions,
            @ShellOption(help = "number of JSON payload items (0 disables)", defaultValue = "1") int payloadItems,
            @ShellOption(help = "execution duration (expression)", defaultValue = "30m") String duration,
            @ShellOption(help = "event batch size", defaultValue = "64") String batchSize,
            @ShellOption(help = "dry run", defaultValue = "false") boolean dryRun
    ) {
        int batchSizeNum = Multiplier.parseInt(batchSize);
        Duration runtimeDuration = DurationFormat.parseDuration(duration);

        console.green(">> Starting events workload <<\n");
        console.yellow("Number of threads: %d\n", threads);
        console.yellow("Number of partitions: %d\n", partitions);
        console.yellow("Number of payload items: %d\n", payloadItems);
        console.yellow("Runtime duration: %s\n", duration);
        console.yellow("Batch size: %d\n", batchSizeNum);

        IntStream.rangeClosed(1, partitions).forEach(p -> {
            IntStream.rangeClosed(1, threads).forEach(t -> {
                boundedExecutor.submit(() -> submitBatch(p, batchSizeNum, payloadItems, dryRun),
                        "writer #" + p + " thread " + t
                                + " (batch size " + batchSize + ")", runtimeDuration);
            });
        });
    }

    private void submitBatch(int partition, int batchSize, int payloadItems, boolean dryRun) {
        final List<OutboxEvent> outboxEvents = IntStream.rangeClosed(1, batchSize)
                .mapToObj(seq -> newOutboxEvent(payloadItems))
                .collect(Collectors.toList());

        final String sql =
                String.format("INSERT INTO event_%d (aggregate_type,aggregate_id,event_type,payload) "
                        + "VALUES (?,?,?,?)", partition);

        if (!dryRun) {
            jdbcTemplate.batchUpdate(sql,
                    new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            OutboxEvent outboxEvent = outboxEvents.get(i);
                            ps.setString(1, outboxEvent.getAggregateType());
                            ps.setString(2, outboxEvent.getAggregateId());
                            ps.setString(3, outboxEvent.getEventType());
                            if (payloadItems > 0) {
                                ps.setObject(4, outboxEvent.getPayload(), Types.OTHER);
                            } else {
                                ps.setNull(4, Types.OTHER);
                            }
                        }

                        @Override
                        public int getBatchSize() {
                            return outboxEvents.size();
                        }
                    });
        }
    }

    private OutboxEvent newOutboxEvent(int payloadItems) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setAggregateId(UUID.randomUUID().toString());
        outboxEvent.setEventType("user_profile_updated");
        outboxEvent.setAggregateType("User");
        if (payloadItems > 0) {
            outboxEvent.setPayload(RandomData.randomUserJson(payloadItems, 2));
        }
        return outboxEvent;
    }
}

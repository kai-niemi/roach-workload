package io.roach.workload.orders;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import io.roach.workload.Profiles;
import io.roach.workload.common.AbstractWorkload;
import io.roach.workload.common.util.DurationFormat;
import io.roach.workload.common.util.Multiplier;
import io.roach.workload.orders.model.AbstractOrder;
import io.roach.workload.orders.model.Order1;
import io.roach.workload.orders.model.OrderEntities;
import io.roach.workload.orders.repository.OrderRepository;

@Profiles.Orders
@ShellComponent
public class OrdersWorkload extends AbstractWorkload {
    @Autowired
    @Qualifier("jdbcOrderRepository")
    private OrderRepository jdbcOrderRepository;

    @Autowired
    @Qualifier("jdbcOrderRepositoryExplicit")
    private OrderRepository jdbcOrderRepositoryExplicit;

    @Autowired
    @Qualifier("jpaOrderRepository")
    private OrderRepository jpaOrderRepository;

    @Override
    public String prompt() {
        return "orders:$ ";
    }

    @ShellMethod(value = "List test")
    public void list(@ShellOption(help = "data access method (jdbc|jdbcx|jpa)", defaultValue = "jdbc") String method,
                     @ShellOption(help = "start offset", defaultValue = "0") int offset,
                     @ShellOption(help = "item limit per page", defaultValue = "50") int limit,
                     @ShellOption(help = "max pages", defaultValue = "500") int pageLimit
    ) {
        OrderRepository repository = getOrderRepositoryUsing(method);
        int page = 1;
        int total = 0;
        List<Order1> orders;
        do {
            orders = repository.findOrders(Order1.class, LocalDate.now(), offset, limit);
            console.yellow("Page %d offset %d limit %d\n", page, offset, limit);
//            orders.forEach(o -> console.green("%s\n", o));
            offset += limit;
            page++;
            total += orders.size();
        } while (!orders.isEmpty() && page < pageLimit);

        console.yellow("Orders in total %,d\n", total);
    }

    @ShellMethod(value = "Initialize orders workload")
    public void init(
            @ShellOption(help = "number of partitions", defaultValue = "10") int partitions,
            @ShellOption(help = "country code", defaultValue = "USA") String countryCode,
            @ShellOption(help = "drop tables before creating", defaultValue = "false") boolean drop) {
        SchemaSupport schemaSupport = new SchemaSupport(dataSource);
        if (drop) {
            console.yellow("Dropping %d tables\n", partitions);
            schemaSupport.dropSchema(partitions);
        }
        console.yellow("Creating %d tables with country code %s\n", partitions, countryCode);
        schemaSupport.createSchema(partitions, countryCode);
    }

    @ShellMethod(value = "Run orders workload")
    public void run(
            @ShellOption(help = "number of partitions (tables)", defaultValue = "10") int partitions,
            @ShellOption(help = "number of write threads", defaultValue = "-1") int writeThreads,
            @ShellOption(help = "number of read threads", defaultValue = "0") int readThreads,
            @ShellOption(help = "queue capacity (default is unbounded)", defaultValue = "-1") int queueSize,
            @ShellOption(help = "order batch size", defaultValue = "16") String batchSize,
            @ShellOption(help = "execution duration", defaultValue = "30m") String duration,
            @ShellOption(help = "data access method (jdbc|jdbcx|jpa)", defaultValue = "jdbc") String method,
            @ShellOption(help = "include JSON payload", defaultValue = "false") boolean includeJson,
            @ShellOption(help = "dry run (disable SQL ops)", defaultValue = "false") boolean dryRun
    ) {
        final int batchSizeNum = Multiplier.parseInt(batchSize);
        final Duration runtimeDuration = DurationFormat.parseDuration(duration);
        final OrderRepository orderRepository = getOrderRepositoryUsing(method);

        if ("jpa".equalsIgnoreCase(method) && partitions > SchemaSupport.orderEntities.size()) {
            console.red("Max %d partitions for JPA mode\n", SchemaSupport.orderEntities.size());
            return;
        }


        if (writeThreads <= 0) {
            writeThreads = Runtime.getRuntime().availableProcessors();
        }

        if (queueSize <= 0) {
            queueSize = Integer.MAX_VALUE;
        }

        console.green(">> Starting orders workload\n");
        console.yellow("Number of tables: %d\n", partitions);
        console.yellow("Number of write threads: %d\n", writeThreads);
        console.yellow("Number of read threads: %d\n", readThreads);
        console.yellow("Queue size: %d\n", queueSize);
        console.yellow("Batch size: %d\n", batchSizeNum);
        console.yellow("Include JSON payload: %s\n", includeJson);
        console.yellow("Runtime duration: %s\n", duration);
        console.yellow("Data access method: %s\n", method);
        console.yellow("Dry run: %s\n", dryRun);

        final LinkedBlockingQueue<List<? extends AbstractOrder>> inBox = new LinkedBlockingQueue<>(queueSize);
        final LinkedBlockingQueue<List<? extends AbstractOrder>> outBox = new LinkedBlockingQueue<>(queueSize);

        // Producer
        boundedExecutor.submit(() -> {
            IntStream.rangeClosed(1, partitions).forEach(value -> {
                Class<? extends AbstractOrder> orderType = SchemaSupport.orderEntities.get(value - 1);
                inBox.offer(OrderEntities.generateOrderEntities(orderType, batchSizeNum, includeJson));
            });
        }, "producer (" + partitions + " tables)", runtimeDuration);

        // Consumers
        IntStream.rangeClosed(1, writeThreads).forEach(value -> {
            boundedExecutor.submit(() -> {
                try {
                    List<? extends AbstractOrder> orderBatch = inBox.take();
                    if (!dryRun) {
                        orderRepository.insertOrders(orderBatch);
                    }
                    outBox.offer(orderBatch);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "writer #" + value + " (batch size " + batchSize + ")", runtimeDuration);
        });

        IntStream.rangeClosed(1, readThreads).forEach(value -> {
            boundedExecutor.submit(() -> {
                try {
                    List<? extends AbstractOrder> orderBatch = outBox.take();
                    if (!dryRun && orderBatch.size() > 0) {
                        orderRepository.readOrders(orderBatch.get(0).getClass(), orderBatch);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "reader #" + value + " (batch size " + batchSize + ")", runtimeDuration);
        });
    }

    private OrderRepository getOrderRepositoryUsing(String method) {
        if ("jdbc".equalsIgnoreCase(method)) {
            return jdbcOrderRepository;
        } else if ("jdbcx".equalsIgnoreCase(method)) {
            return jdbcOrderRepositoryExplicit;
        } else if ("jpa".equalsIgnoreCase(method)) {
            return jpaOrderRepository;
        }
        throw new IllegalArgumentException("Unknown access method (jdbc|jdbcx|jpa): " + method);
    }
}

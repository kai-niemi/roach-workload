package io.roach.workload.common.cli;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.LongSummaryStatistics;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import javax.annotation.PostConstruct;

import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.ReflectionUtils;

import com.zaxxer.hikari.HikariDataSource;

import io.roach.workload.common.jpa.DataSourceHelper;
import io.roach.workload.common.util.BoundedExecutor;
import io.roach.workload.common.util.CallMetric;

import static io.roach.workload.common.config.DataSourceConfig.SQL_TRACE_LOGGER;

@ShellComponent
@ShellCommandGroup("Metrics Commands")
public class Metrics {
    private static final Semaphore mutex = new Semaphore(1);

    private static final ConcurrentLinkedDeque<ConnectionPoolStats> aggregatedConnectionPoolStats
            = new ConcurrentLinkedDeque<>();

    private static final ConcurrentLinkedDeque<ThreadPoolStats> aggregatedThreadPoolStats
            = new ConcurrentLinkedDeque<>();

    private static final ConcurrentLinkedDeque<Double> aggregatedLoadAvg
            = new ConcurrentLinkedDeque<>();

    private boolean printMetrics = true;

    @Autowired
    private ScheduledExecutorService scheduledExecutorService;

    @Autowired
    private HikariDataSource hikariDataSource;

    @Autowired
    private Console console;

    @Autowired
    private BoundedExecutor boundedExecutor;

    @ShellMethod(value = "Toggle console metrics", key = {"metrics", "m"})
    public void toggleMetrics() {
        printMetrics = !printMetrics;
        console.green("Metrics printing is %s\n", printMetrics ? "on" : "off");
    }

    @ShellMethod(value = "Toggle SQL tracing (workload.log)", key = {"trace", "t"})
    public void toggleTrace() {
        ch.qos.logback.classic.LoggerContext loggerContext = (ch.qos.logback.classic.LoggerContext) LoggerFactory
                .getILoggerFactory();
        ch.qos.logback.classic.Logger logger = loggerContext.getLogger(SQL_TRACE_LOGGER);
        if (logger.getLevel().isGreaterOrEqual(ch.qos.logback.classic.Level.DEBUG)) {
            logger.setLevel(ch.qos.logback.classic.Level.TRACE);
            console.green("SQL trace logging enabled to 'workload.log'\n");
            logger.trace("Enabled");
        } else {
            logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
            logger.debug("Disabled");
            console.green("SQL trace logging disabled\n");
        }
    }

    @PostConstruct
    public void init() {
        scheduledExecutorService.scheduleAtFixedRate(poolMetricsSampler(), 5, 1, TimeUnit.SECONDS);
        scheduledExecutorService.scheduleAtFixedRate(poolMetricsPrinter(), 10, 30, TimeUnit.SECONDS);
        scheduledExecutorService.scheduleAtFixedRate(callMetricsPrinter(), 5, 3, TimeUnit.SECONDS);
    }

    private Runnable callMetricsPrinter() {
        return () -> {
            if (printMetrics && boundedExecutor.hasActiveWorkers()) {
                try {
                    mutex.acquire();

                    CallMetric callMetric = boundedExecutor.getCallMetric();
                    console.magenta("%s\n", callMetric.prettyPrintHeader());
                    AtomicBoolean toggle = new AtomicBoolean();
                    callMetric.prettyPrintBody(s -> {
                        if (toggle.getAndSet(!toggle.get())) {
                            console.yellow("%s\n", s);
                        } else {
                            console.bright("%s\n", s);
                        }
                    });
                    console.green("%s\n", callMetric.prettyPrintFooter());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    mutex.release();
                }
            }
        };
    }

    private Runnable poolMetricsSampler() {
        return () -> {
            aggregatedConnectionPoolStats.add(ConnectionPoolStats.from(hikariDataSource.getHikariPoolMXBean()));
            aggregatedThreadPoolStats.add(ThreadPoolStats.from(boundedExecutor));

            OperatingSystemMXBean mxBean = ManagementFactory.getOperatingSystemMXBean();
            if (mxBean.getSystemLoadAverage() != -1) {
                aggregatedLoadAvg.add(mxBean.getSystemLoadAverage());
            }
        };
    }

    private Runnable poolMetricsPrinter() {
        return () -> {
            if (!printMetrics || !boundedExecutor.hasActiveWorkers()) {
                return;
            }

            try {
                mutex.acquire();

                ConnectionPoolStats poolStats = aggregatedConnectionPoolStats.peekLast();
                if (poolStats != null) {
                    console.magenta("Connection Pool:\n");
                    printSummaryStats("active",
                            poolStats.activeConnections,
                            aggregatedConnectionPoolStats.stream().mapToInt(value -> value.activeConnections));
                    printSummaryStats("idle",
                            poolStats.idleConnections,
                            aggregatedConnectionPoolStats.stream().mapToInt(value -> value.idleConnections));
                    printSummaryStats("waiting",
                            poolStats.threadsAwaitingConnection,
                            aggregatedConnectionPoolStats.stream().mapToInt(value -> value.threadsAwaitingConnection));
                    printSummaryStats("total",
                            poolStats.totalConnections,
                            aggregatedConnectionPoolStats.stream().mapToInt(value -> value.totalConnections));
                }

                ThreadPoolStats threadPoolStats = aggregatedThreadPoolStats.peekLast();
                if (threadPoolStats != null) {
                    console.magenta("Thread Pool:\n");
                    printSummaryStats("poolSize",
                            threadPoolStats.poolSize,
                            aggregatedThreadPoolStats.stream().mapToInt(value -> value.poolSize));
                    printSummaryStats("largestPoolSize",
                            threadPoolStats.largestPoolSize,
                            aggregatedThreadPoolStats.stream().mapToInt(value -> value.largestPoolSize));
                    printSummaryStats("activeCount",
                            threadPoolStats.activeCount,
                            aggregatedThreadPoolStats.stream().mapToInt(value -> value.activeCount));
                    printSummaryStats("taskCount",
                            threadPoolStats.taskCount,
                            aggregatedThreadPoolStats.stream().mapToLong(value -> value.taskCount));
                    printSummaryStats("completedTaskCount",
                            threadPoolStats.completedTaskCount,
                            aggregatedThreadPoolStats.stream().mapToLong(value -> value.completedTaskCount));
                }

                if (!aggregatedLoadAvg.isEmpty()) {
                    printSummaryStats("loadavg",
                            aggregatedLoadAvg.getLast(),
                            aggregatedLoadAvg.stream().mapToDouble(value -> value));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Throwable e) {
                console.red(e.toString());
            } finally {
                mutex.release();
            }
        };
    }

    private void printSummaryStats(String label, int current, IntStream histogram) {
        IntSummaryStatistics ss = histogram.summaryStatistics();
        if (ss.getCount() > 0) {
            console.yellow("%20s:", label);
            console.green(" current %d, min %d, max %d, avg %.0f, samples %d\n",
                    current,
                    ss.getMin(),
                    ss.getMax(),
                    ss.getAverage(),
                    ss.getCount());
        }
    }

    private void printSummaryStats(String label, long current, LongStream histogram) {
        LongSummaryStatistics ss = histogram.summaryStatistics();
        if (ss.getCount() > 0) {
            console.yellow("%20s:", label);
            console.green(" current %d, min %d, max %d, avg %.0f, samples %d\n",
                    current,
                    ss.getMin(),
                    ss.getMax(),
                    ss.getAverage(),
                    ss.getCount());
        }
    }

    private void printSummaryStats(String label, double current, DoubleStream histogram) {
        DoubleSummaryStatistics ss = histogram.summaryStatistics();
        if (ss.getCount() > 0) {
            console.yellow("%20s:", label);
            console.green(" current %.1f, min %.1f, max %.1f, avg %.1f, samples %d\n",
                    current,
                    ss.getMin(),
                    ss.getMax(),
                    ss.getAverage(),
                    ss.getCount());
        }
    }

    @ShellMethod(value = "Print system information", key = {"system-info", "si"})
    public void systemInfo() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        console.yellow(">> OS\n");
        console.green(" Arch: %s | OS: %s | Version: %s\n", os.getArch(), os.getName(), os.getVersion());
        console.green(" Available processors: %d\n", os.getAvailableProcessors());
        console.green(" Load avg: %f\n", os.getSystemLoadAverage());

        RuntimeMXBean r = ManagementFactory.getRuntimeMXBean();
        console.yellow(">> Runtime\n");
        console.green(" Uptime: %s\n", r.getUptime());
        console.green(" VM name: %s | Vendor: %s | Version: %s\n", r.getVmName(), r.getVmVendor(), r.getVmVersion());

        ThreadMXBean t = ManagementFactory.getThreadMXBean();
        console.yellow(">> Runtime\n");
        console.green(" Peak threads: %d\n", t.getPeakThreadCount());
        console.green(" Thread #: %d\n", t.getThreadCount());
        console.green(" Total started threads: %d\n", t.getTotalStartedThreadCount());

        Arrays.stream(t.getAllThreadIds()).sequential().forEach(value -> {
            console.green(" Thread (%d): %s %s\n", value,
                    t.getThreadInfo(value).getThreadName(),
                    t.getThreadInfo(value).getThreadState().toString()
            );
        });

        MemoryMXBean m = ManagementFactory.getMemoryMXBean();
        console.yellow(">> Memory\n");
        console.green(" Heap: %s\n", m.getHeapMemoryUsage().toString());
        console.green(" Non-heap: %s\n", m.getNonHeapMemoryUsage().toString());
        console.green(" Pending GC: %s\n", m.getObjectPendingFinalizationCount());
    }

    @ShellMethod(value = "Print datasource and DB information", key = {"db-info", "di"})
    @ShellMethodAvailability("dataSourceCheck")
    public void dbInfo(@ShellOption(help = "print all JDBC metadata", defaultValue = "false") boolean verbose) {
        console.yellow("HikariCP data source\n");
        console.green("\tjdbc url: %s\n", hikariDataSource.getJdbcUrl());
        console.green("\tjdbc user: %s\n", hikariDataSource.getUsername());
        console.green("\tmax pool size: %s\n", hikariDataSource.getMaximumPoolSize());
        console.green("\tmin idle size: %s\n", hikariDataSource.getMinimumIdle());
        console.green("\tidle timeout: %s\n", hikariDataSource.getIdleTimeout());
        console.green("\tmax lifetime: %s\n", hikariDataSource.getMaxLifetime());
        console.green("\tvalidation timeout: %s\n", hikariDataSource.getValidationTimeout());
        console.green("\tconnection init: %s\n", hikariDataSource.getConnectionInitSql());

        console.yellow("Database metadata:\n");
        console.green("\tdatabaseVersion: %s\n", DataSourceHelper.databaseVersion(hikariDataSource));

        try (Connection connection = DataSourceUtils.doGetConnection(hikariDataSource)) {
            DatabaseMetaData metaData = connection.getMetaData();

            if (verbose) {
                Arrays.stream(ReflectionUtils.getDeclaredMethods(metaData.getClass())).sequential().forEach(method -> {
                    if (method.getParameterCount() == 0) {
                        try {
                            console.green("\t%s: %s\n", method.getName(),
                                    ReflectionUtils.invokeMethod(method, metaData));
                        } catch (Exception e) {
                            console.red(e.toString());
                        }
                    }
                });
            } else {
                console.green("\tautoCommit: %s\n", connection.getAutoCommit());
                console.green("\tdatabaseProductName: %s\n", metaData.getDatabaseProductName());
                console.green("\tdatabaseMajorVersion: %s\n", metaData.getDatabaseMajorVersion());
                console.green("\tdatabaseMinorVersion: %s\n", metaData.getDatabaseMinorVersion());
                console.green("\tdatabaseProductVersion: %s\n", metaData.getDatabaseProductVersion());
                console.green("\tdriverMajorVersion: %s\n", metaData.getDriverMajorVersion());
                console.green("\tdriverMinorVersion: %s\n", metaData.getDriverMinorVersion());
                console.green("\tdriverName: %s\n", metaData.getDriverName());
                console.green("\tdriverVersion: %s\n", metaData.getDriverVersion());
                console.green("\tmaxConnections: %s\n", metaData.getMaxConnections());
                console.green("\tdefaultTransactionIsolation: %s\n", metaData.getDefaultTransactionIsolation());
                console.green("\ttransactionIsolation: %s\n", connection.getTransactionIsolation());
                console.green("\ttransactionIsolationName: %s\n",
                        ConnectionProviderInitiator.toIsolationNiceName(connection.getTransactionIsolation()));
            }
        } catch (SQLException ex) {
            console.red(ex.toString());
        }
    }
}

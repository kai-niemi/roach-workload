package io.roach.workload.common.cli;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import io.roach.workload.common.util.BoundedExecutor;

@ShellComponent
@ShellCommandGroup("Connection and Thread Pool Commands")
public class Pools {
    @Autowired
    private HikariDataSource hikariDataSource;

    @Autowired
    private Console console;

    @Autowired
    private BoundedExecutor boundedExecutor;

    @Autowired
    private ScheduledExecutorService scheduledExecutorService;

    @ShellMethod(value = "Configure connection pool size", key = {"conn-pool-set", "cps"})
    @ShellMethodAvailability("noActiveWorkersCheck")
    public void poolSizeSet(
            @ShellOption(help = "connection pool max size (guide: 4x vCPUs / n:of pools)", defaultValue = "40") int maxSize,
            @ShellOption(help = "connection pool min idle size", defaultValue = "10") int minSize) {
        console.green("Setting max pool size %d and min idle to %d\n", maxSize, minSize);
        hikariDataSource.setMaximumPoolSize(maxSize);
        hikariDataSource.setMinimumIdle(minSize);
    }

    @ShellMethod(value = "Print connection pool information", key = {"conn-pool-get", "cpg"})
    @ShellMethodAvailability("dataSourceCheck")
    public void poolSizeGet(@ShellOption(help = "repeat period in seconds", defaultValue = "0") int repeatTime) {
        Runnable r = () -> {
            HikariPoolMXBean poolInfo = hikariDataSource.getHikariPoolMXBean();
            console.yellow("Connection pool status:\n");
            console.green("\tactiveConnections: %s\n", poolInfo.getActiveConnections());
            console.green("\tidleConnections: %s\n", poolInfo.getIdleConnections());
            console.green("\ttotalConnections: %s\n", poolInfo.getTotalConnections());
            console.green("\tthreadsAwaitingConnection: %s\n", poolInfo.getThreadsAwaitingConnection());

            HikariConfigMXBean configInfo = hikariDataSource.getHikariConfigMXBean();
            console.yellow("Connection pool configuration:\n");
            console.green("\tmaximumPoolSize: %s\n", configInfo.getMaximumPoolSize());
            console.green("\tminimumIdle: %s\n", configInfo.getMinimumIdle());
            console.green("\tconnectionTimeout: %s\n", configInfo.getConnectionTimeout());
            console.green("\tvalidationTimeout: %s\n", configInfo.getValidationTimeout());
            console.green("\tidleTimeout: %s\n", configInfo.getIdleTimeout());
            console.green("\tmaxLifetime: %s\n", configInfo.getMaxLifetime());
            console.green("\tpoolName: %s\n", configInfo.getPoolName());
            console.green("\tleakDetectionThreshold: %s\n", configInfo.getLeakDetectionThreshold());
            console.green("\tcatalog: %s\n", configInfo.getCatalog());
        };

        if (repeatTime > 0) {
            ScheduledFuture<?> f = scheduledExecutorService
                    .scheduleAtFixedRate(r, 0, 2, TimeUnit.SECONDS);
            scheduledExecutorService
                    .schedule(() -> {
                        f.cancel(true);
                    }, repeatTime, TimeUnit.SECONDS);
        } else {
            r.run();
        }
    }

    @ShellMethod(value = "Configure thread pool size", key = {"thread-pool-set", "tps"})
    @ShellMethodAvailability("noActiveWorkersCheck")
    public void threadPoolSet(
            @ShellOption(help = "core thread pool size (guide: 2x vCPUs of host)") int size,
            @ShellOption(help = "thread queue size (guide: 2x pool size)", defaultValue = "-1") int queueSize) {
        if (queueSize < 0) {
            queueSize = size * 2;
        }
        if (queueSize < size) {
            throw new IllegalArgumentException("Queue size must be >= thread size");
        }
        console.green("Setting thread pool size to %d queue size %d\n", size, queueSize);
        boundedExecutor.cancelAndRestart(size, queueSize);
    }

    @ShellMethod(value = "Print thread pool information", key = {"thread-pool-get", "tpg"})
    public void threadPoolGet(@ShellOption(help = "repeat period in seconds", defaultValue = "0") int repeatTime) {
        Runnable r = () -> {
            ThreadPoolStats stats = ThreadPoolStats.from(boundedExecutor);
            console.yellow("Thread pool status:\n");
            console.green("\tpoolSize: %s\n", stats.poolSize);
            console.green("\tmaximumPoolSize: %s\n", stats.maximumPoolSize);
            console.green("\tcorePoolSize: %s\n", stats.corePoolSize);
            console.green("\tactiveCount: %s\n", stats.activeCount);
            console.green("\tcompletedTaskCount: %s\n", stats.completedTaskCount);
            console.green("\ttaskCount: %s\n", stats.taskCount);
            console.green("\tlargestPoolSize: %s\n", stats.largestPoolSize);
        };

        if (repeatTime > 0) {
            ScheduledFuture<?> f = scheduledExecutorService
                    .scheduleAtFixedRate(r, 0, 2, TimeUnit.SECONDS);
            scheduledExecutorService
                    .schedule(() -> {
                        f.cancel(true);
                    }, repeatTime, TimeUnit.SECONDS);
        } else {
            r.run();
        }
    }
}

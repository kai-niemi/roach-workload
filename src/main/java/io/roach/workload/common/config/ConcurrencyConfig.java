package io.roach.workload.common.config;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.roach.workload.common.util.BoundedExecutor;

@Configuration
public class ConcurrencyConfig {
    @Value("${roach.thread-pool-size}")
    private int poolSize;

    @Value("${roach.thread-queue-size}")
    private int queueSize;

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService scheduledExecutor() {
        return Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @Bean(destroyMethod = "shutdown")
    public BoundedExecutor boundedExecutor() {
        return new BoundedExecutor(corePoolSize(), queueSize());
    }

    private int corePoolSize() {
        return poolSize > 0 ? poolSize : Runtime.getRuntime().availableProcessors() * 4;
    }

    private int queueSize() {
        return queueSize > 0 ? queueSize : corePoolSize() * 4;
    }
}

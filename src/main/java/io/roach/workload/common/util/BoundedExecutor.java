package io.roach.workload.common.util;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.TransactionSystemException;

public class BoundedExecutor {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final CallMetric callMetric = new CallMetric();

    private int corePoolSize;

    private int queueSize;

    private Semaphore semaphore;

    private ThreadPoolExecutor executorService;

    public BoundedExecutor(int corePoolSize, int queueSize) {
        this.corePoolSize = corePoolSize;
        this.queueSize = queueSize;
        this.semaphore = new Semaphore(queueSize);
        this.executorService = newExecutorService(corePoolSize);
    }

    private ThreadPoolExecutor newExecutorService(int poolSize) {
        return new ThreadPoolExecutor(poolSize,
                Integer.MAX_VALUE,
                0,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<>());
    }

    public CallMetric getCallMetric() {
        return callMetric;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public <V> Future<V> submit(final Runnable task,
                                final String groupName,
                                final Duration duration) {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to acquire semaphore", e);
        }

        try {
            return executorService.submit(() -> {
                final CallMetric.Context context = callMetric.add(groupName);
                try {
                    final long startTime = System.currentTimeMillis();
                    do {
                        long time = context.enter();
                        try {
                            task.run();
                            context.exit(time, null);
                        } catch (TransientDataAccessException | TransactionSystemException e) {
                            context.exit(time, e);
                            logger.warn("Transient error for " + groupName, e);
                        } catch (Exception e) {
                            context.exit(time, e);
                            logger.error("Non-transient error - cancelling " + groupName, e);
                            break;
                        }
                    } while (System.currentTimeMillis() - startTime < duration.toMillis()
                            && !Thread.interrupted()
                            && !isShutdown());
                } finally {
                    semaphore.release();
                    callMetric.remove(groupName);
                }
                return null;
            });
        } catch (RejectedExecutionException e) {
            semaphore.release();
            return null;
        }
    }

    public void shutdown() {
        logger.info("Shutdown with {} active workers", activeWorkers());
        executorService.shutdown();
    }

    public void cancelAndRestart(int corePoolSize, int queueSize) {
        this.corePoolSize = corePoolSize;
        this.queueSize = queueSize;

        cancelAndRestart();
    }

    public void cancelAndRestart() {
        if (semaphore.hasQueuedThreads()) {
            semaphore.drainPermits();
        }

        logger.info("Terminating {} active workers - awaiting completion", activeWorkers());
        this.executorService.shutdownNow();

        try {
            while (!this.executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.info("Awaiting termination of {} workers", activeWorkers());
            }

            logger.info("All workers terminated");

            if (!this.executorService.isTerminated()) {
                throw new IllegalStateException();
            }

            this.callMetric.clear();

            logger.info("Creating new thread pool with core size {} and queue size {}", corePoolSize, queueSize);

            this.semaphore = new Semaphore(queueSize);
            this.executorService = newExecutorService(corePoolSize);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    public boolean hasActiveWorkers() {
        return activeWorkers() > 0 || semaphore.hasQueuedThreads();
    }

    public int activeWorkers() {
        return executorService.getActiveCount();
    }
}

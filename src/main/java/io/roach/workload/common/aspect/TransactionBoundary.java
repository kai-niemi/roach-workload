package io.roach.workload.common.aspect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Indicates the annotated class or method is a transactional boundary. It's architectural role is to
 * delegate to control services or repositories to perform actual business logic processing in
 * the context of a new transaction.
 * <p/>
 * Marks the annotated class as {@link org.springframework.transaction.annotation.Transactional @Transactional}
 * with propagation level {@link org.springframework.transaction.annotation.Propagation#REQUIRES_NEW REQUIRES_NEW},
 * clearly indicating that a new transaction is started before method entry.
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Transactional(propagation = Propagation.REQUIRES_NEW)
public @interface TransactionBoundary {
    /**
     * @return number of times to retry aborted transient data access exceptions
     * with exponential backoff (up to 5s per cycle). Zero or negative value disables retries.
     */
    int retryAttempts() default 10;

    /**
     * @return max backoff time in millis
     */
    long maxBackoff() default 30_000;

    /**
     * @return transaction read-only hint optimization
     */
    boolean readOnly() default false;

    /**
     * @return enable follower reads (read-only transaction implied)
     */
    boolean followerRead() default false;

    /**
     * The amount of time (seconds) a statement can run before being stopped.
     *
     * @return statement timeout in seconds
     */
    String statementTimeout() default "";

    /**
     * Automatically terminates sessions that are idle in a transaction past the specified threshold (seconds).
     *
     * @return when set to 0s, the session will not timeout
     */
    String idleInTransactionSessionTimeout() default "";

    /**
     * Combined transaction timeout that sets statement and idle-in-transaction to same value.
     *
     * @return when set to 0s, the transaction will not timeout
     */
    String transactionTimeout() default "";

    /**
     * @return sets the transaction priority
     */
    Priority priority() default Priority.normal;

    enum Priority {
        normal,
        low,
        high
    }
}

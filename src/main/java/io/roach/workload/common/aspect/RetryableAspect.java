package io.roach.workload.common.aspect;

import java.lang.reflect.UndeclaredThrowableException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 20) // This advisor must be before the TX advisor in the call chain
public class RetryableAspect {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Around(value = "Pointcuts.anyTransactionBoundaryOperation(transactionBoundary)",
            argNames = "pjp,transactionBoundary")
    public Object doInTransaction(ProceedingJoinPoint pjp, TransactionBoundary transactionBoundary)
            throws Throwable {
        Assert.isTrue(!TransactionSynchronizationManager.isActualTransactionActive(), "TX active");

        // Grab from type if needed (for non-annotated methods)
        if (transactionBoundary == null) {
            transactionBoundary = AnnotationUtils
                    .findAnnotation(pjp.getSignature().getDeclaringType(), TransactionBoundary.class);
        }

        int numCalls = 0;

        final Instant callTime = Instant.now();

        do {
            Throwable t;
            try {
                numCalls++;
                Object rv = pjp.proceed();
                if (numCalls > 1) {
                    logger.debug(
                            "Transient error recovered after " + numCalls + " of "
                                    + transactionBoundary.retryAttempts() + " retries ("
                                    + Duration.between(callTime, Instant.now()).toString() + ")");
                }
                return rv;
            } catch (DataAccessException | TransactionException ex) {
                t = ex;
            } catch (UndeclaredThrowableException ex) {
                Throwable ut = ex.getUndeclaredThrowable();
                while (ut instanceof UndeclaredThrowableException) {
                    ut = ((UndeclaredThrowableException) ut).getUndeclaredThrowable();
                }
                t = ut;
            }

            Throwable cause = NestedExceptionUtils.getMostSpecificCause(t);
            if (cause instanceof SQLException) {
                SQLException sqlException = (SQLException) cause;
                // 08006 - tx timeout?
                // 57014 - stmt timeout?
                if ("40001".equals(sqlException.getSQLState())) { // Transient error code
                    handleTransientException(sqlException, numCalls, pjp.getSignature().toShortString(),
                            transactionBoundary.maxBackoff());
                    continue;
                }

                // Non-transient and futile
                logger.warn("Non-transient SQL error state: {} code: {} message: {}",
                        sqlException.getSQLState(), sqlException.getErrorCode(), sqlException.getMessage());
                SQLException next = sqlException.getNextException();
                while (next != null) {
                    logger.warn("Non-transient SQL error state: {} code: {} message: {}",
                            sqlException.getSQLState(), sqlException.getErrorCode(), sqlException.getMessage());
                    next = sqlException.getNextException();
                }
            }
            throw t; // Propagate up the stack
        } while (numCalls < transactionBoundary.retryAttempts());

        throw new ConcurrencyFailureException("Too many transient errors (" + numCalls + ") for method ["
                + pjp.getSignature().toShortString() + "]. Giving up!");
    }

    private void handleTransientException(SQLException ex, int numCalls, String method, long maxBackoff) {
        try {
            long backoffMillis = Math.min((long) (Math.pow(2, numCalls) + Math.random() * 1000), maxBackoff);
            if (numCalls <= 1 && logger.isWarnEnabled()) {
                logger.warn("Transient error (backoff {}ms) in call {} to '{}': {}",
                        backoffMillis, numCalls, method, ex.getMessage());
            }
            Thread.sleep(backoffMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}


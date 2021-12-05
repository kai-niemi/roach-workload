package io.roach.workload.common.aspect;

import java.util.Objects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 1) // This advisor must be after the TX advisor in the call chain
public class SessionHintsAspect {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Around(value = "Pointcuts.anyTransactionBoundaryOperation(transactionBoundary)",
            argNames = "pjp,transactionBoundary")
    public Object doInTransaction(ProceedingJoinPoint pjp, TransactionBoundary transactionBoundary)
            throws Throwable {
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(), "TX not active");

        if (transactionBoundary == null) {
            transactionBoundary = AnnotationUtils
                    .findAnnotation(pjp.getSignature().getDeclaringType(), TransactionBoundary.class);
        }

        Objects.requireNonNull(transactionBoundary);

        if (logger.isTraceEnabled()) {
            logger.trace("Transaction attributes applied for {}: {}",
                    pjp.getSignature().toShortString(),
                    transactionBoundary);
        }

        applyVariables(transactionBoundary);

        return pjp.proceed();
    }

    private void applyVariables(TransactionBoundary transactionBoundary) {
        if (!"".equals(transactionBoundary.transactionTimeout())) {
            jdbcTemplate.update("SET idle_in_transaction_session_timeout=?",
                    transactionBoundary.transactionTimeout());
            jdbcTemplate.update("SET statement_timeout=?",
                    transactionBoundary.transactionTimeout());
        } else {
            if (!"".equals(transactionBoundary.statementTimeout())) {
                jdbcTemplate.update("SET statement_timeout=?",
                        transactionBoundary.statementTimeout());
            }

            if (!"".equals(transactionBoundary.idleInTransactionSessionTimeout())) {
                jdbcTemplate.update("SET idle_in_transaction_session_timeout=?",
                        transactionBoundary.idleInTransactionSessionTimeout());
            }
        }

        if (!TransactionBoundary.Priority.normal.equals(transactionBoundary.priority())) {
            jdbcTemplate.execute("SET TRANSACTION PRIORITY " + transactionBoundary.priority().name());
        }

        if (transactionBoundary.readOnly()) {
            jdbcTemplate.execute("SET transaction_read_only=true");
        }

        if (transactionBoundary.followerRead()) {
            jdbcTemplate.execute("SET TRANSACTION AS OF SYSTEM TIME follower_read_timestamp()");
        }
    }
}


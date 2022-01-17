package io.roach.workload.orders.repository;

import java.time.LocalDate;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.roach.workload.Profiles;
import io.roach.workload.common.aspect.TransactionBoundary;
import io.roach.workload.orders.model.AbstractOrder;

@Repository
@Profiles.Orders
public class JdbcOrderRepositoryExplicit extends JdbcOrderRepository {
    public JdbcOrderRepositoryExplicit(@Autowired DataSource dataSource) {
        super(dataSource);
    }

    @Override
    @TransactionBoundary
    public void insertOrders(List<? extends AbstractOrder> orders, boolean includeJson) {
        super.insertOrders(orders, includeJson);
    }

    @Transactional(propagation = Propagation.NEVER)
    @Override
    public List readOrders(Class<? extends AbstractOrder> orderType, List<? extends AbstractOrder> templates) {
        return super.readOrders(orderType, templates);
    }

    @Override
    @TransactionBoundary(statementTimeout = "10s", idleInTransactionSessionTimeout = "5s", readOnly = true)
    public <T extends AbstractOrder> List<T> findOrders(Class<T> orderType, LocalDate moreRecentThan, int offset,
                                                        int limit) {
        return super.findOrders(orderType, moreRecentThan, offset, limit);
    }
}

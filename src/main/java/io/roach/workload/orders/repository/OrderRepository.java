package io.roach.workload.orders.repository;

import java.time.LocalDate;
import java.util.List;

import io.roach.workload.orders.model.AbstractOrder;

public interface OrderRepository {
    void insertOrders(List<? extends AbstractOrder> orders);

    List<? extends AbstractOrder> readOrders(Class<? extends AbstractOrder> orderType,
                    List<? extends AbstractOrder> templates);

    default <T extends AbstractOrder> List<T> findOrders(Class<T> orderType,
                                                         LocalDate moreRecentThan,
                                                         int offset,
                                                         int limit) {
        throw new UnsupportedOperationException();
    }
}

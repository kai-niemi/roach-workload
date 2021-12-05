package io.roach.workload.orders.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import io.roach.workload.Profiles;
import io.roach.workload.orders.model.AbstractOrder;

@Repository
@Profiles.Orders
public class FakeOrderRepository implements OrderRepository {
    @Override
    public void insertOrders(List<? extends AbstractOrder> orders) {
    }

    @Override
    public List<? extends AbstractOrder> readOrders(Class<? extends AbstractOrder> orderType,
                                                    List<? extends AbstractOrder> templates) {
        return templates;
    }
}

package io.roach.workload.orders.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Session;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.roach.workload.Profiles;
import io.roach.workload.orders.model.AbstractOrder;

@Repository
@Profiles.Orders
public class JpaOrderRepository implements OrderRepository {
    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertOrders(List<? extends AbstractOrder> orders, boolean includeJson) {
        // Default is 64
        if (orders.size() != 64) {
            Session session = em.unwrap(Session.class);
            session.setJdbcBatchSize(orders.size());
        }
        orders.forEach(order -> {
            if (!includeJson) {
                order.setCustomer(null);
            }
            em.persist(order);
        }); // transparent
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public List<? extends AbstractOrder> readOrders(Class<? extends AbstractOrder> orderType, List<? extends AbstractOrder> templates) {
        String entityName = orderType.getSimpleName();

        List<AbstractOrder.Id> ids = new ArrayList<>(templates.size());
        templates.forEach(t -> ids.add(AbstractOrder.Id.of(t.getId().getUUID(), t.getId().getDatePlaced())));

        // Qualify type to avoid union all
        List<? extends AbstractOrder> orders = em
                .createQuery("select o from " + entityName + " o where type(o) = :orderType and o.id in (:ids)", orderType)
                .setParameter("orderType", orderType)
                .setParameter("ids", ids)
                .getResultList();

        if (orders.size() != ids.size()) {
            throw new IncorrectResultSizeDataAccessException(ids.size(), orders.size());
        }

        return orders;
    }

    @Override
    public <T extends AbstractOrder> List<T> findOrders(Class<T> orderType,
                                                        LocalDate moreRecentThan, int offset, int limit) {
        String entityName = orderType.getSimpleName();
        // Qualify type to avoid union all
        return em.createQuery("select o from " + entityName + " o where type(o) = :orderType and o.datePlaced >= :date", orderType)
                .setParameter("orderType", orderType)
                .setParameter("date", moreRecentThan)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }
}

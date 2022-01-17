package io.roach.workload.orders.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.persistence.Table;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.roach.workload.Profiles;
import io.roach.workload.common.util.Money;
import io.roach.workload.orders.model.AbstractOrder;
import io.roach.workload.orders.model.Address;
import io.roach.workload.orders.model.Country;
import io.roach.workload.orders.model.Customer;
import io.roach.workload.orders.model.ShipmentStatus;

@Repository
@Profiles.Orders
public class JdbcOrderRepository implements OrderRepository {
    private final JdbcTemplate jdbcTemplate;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public JdbcOrderRepository(@Autowired DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    private String findTableName(Class<?> order) {
        Table t = AnnotationUtils.findAnnotation(order, Table.class);
        if (t == null) {
            throw new IllegalArgumentException("No @Table annotation found for: " + order.getName());
        }
        return t.name();
    }

    @Override
    public void insertOrders(List<? extends AbstractOrder> orders, boolean includeJson) {
        if (orders.isEmpty()) {
            return;
        }

        String tableName = findTableName(orders.get(0).getClass());

        final String query = "INSERT INTO " + tableName + " ("
                + "id,"
                + "order_number,"
                + "bill_address1,"
                + "bill_address2,"
                + "bill_city,"
                + "bill_country_name,"
                + "bill_postcode,"
                + "bill_to_first_name,"
                + "bill_to_last_name,"
                + "deliv_to_first_name,"
                + "deliv_to_last_name,"
                + "deliv_address1,"
                + "deliv_address2,"
                + "deliv_city,"
                + "deliv_country_name,"
                + "deliv_postcode,"
                + "status,"
                + "amount,"
                + "currency,"
                + "customer_id,"
                + "payment_method_id,"
                + "date_placed,"
                + "date_updated,"
                + "customer_profile)"
                + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        int[] rv = jdbcTemplate.batchUpdate(query, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int entityId) throws SQLException {
                AbstractOrder order = orders.get(entityId);
                int i = 1;
                ps.setObject(i, order.getId().getUUID());
                ps.setInt(++i, order.getOrderNumber());
                ps.setString(++i, order.getBillAddress().getAddress1());
                ps.setString(++i, order.getBillAddress().getAddress2());
                ps.setString(++i, order.getBillAddress().getCity());
                ps.setString(++i, order.getBillAddress().getCountry().getCode());
                ps.setString(++i, order.getBillAddress().getPostcode());
                ps.setString(++i, order.getBillToFirstName());
                ps.setString(++i, order.getBillToLastName());
                ps.setString(++i, order.getDeliverToFirstName());
                ps.setString(++i, order.getDeliverToLastName());
                ps.setString(++i, order.getDeliveryAddress().getAddress1());
                ps.setString(++i, order.getDeliveryAddress().getAddress2());
                ps.setString(++i, order.getDeliveryAddress().getCity());
                ps.setString(++i, order.getDeliveryAddress().getCountry().getCode());
                ps.setString(++i, order.getDeliveryAddress().getPostcode());
                ps.setString(++i, order.getStatus().name());
                ps.setBigDecimal(++i, order.getTotalPrice().getAmount());
                ps.setString(++i, order.getTotalPrice().getCurrency().getCurrencyCode());
                ps.setObject(++i, order.getCustomerId());
                ps.setObject(++i, order.getPaymentMethod());
                ps.setObject(++i, order.getDatePlaced());
                ps.setObject(++i, order.getDateUpdated());

                Customer customer = order.getCustomer();
                if (customer != null && includeJson) {
                    try {
                        ps.setObject(++i,
                                objectMapper.writer().writeValueAsString(customer),
                                java.sql.Types.OTHER);
                    } catch (JsonProcessingException e) {
                        throw new SQLException("Error serializing json", e);
                    }
                } else {
                    ps.setNull(++i, Types.NULL);
                }
            }

            @Override
            public int getBatchSize() {
                return orders.size();
            }
        });

        Arrays.stream(rv).forEach(k -> {
            if (k == PreparedStatement.EXECUTE_FAILED) {
                throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(query, 1, k);
            }
        });
    }

    @Override
    public List<? extends AbstractOrder> readOrders(Class<? extends AbstractOrder> orderType,
                           List<? extends AbstractOrder> templates) {
        String tableName = findTableName(orderType);

        List<Object[]> idTuples = new ArrayList<>(templates.size());
        templates.forEach(order -> {
            AbstractOrder.Id id = order.getId();
            idTuples.add(new Object[] {id.getDatePlaced(), id.getUUID()}); // order!
        });

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("ids", idTuples);

        List<? extends AbstractOrder> orders = namedParameterJdbcTemplate
                .query("SELECT * FROM " + tableName + " WHERE (date_placed,id) IN (:ids)",
                        parameters,
                        orderMapper(orderType)
                );

        if (orders.size() != templates.size()) {
            throw new IncorrectResultSizeDataAccessException(templates.size(), orders.size());
        }

        return orders;
    }

    @Override
    public <T extends AbstractOrder> List<T> findOrders(Class<T> orderType, LocalDate moreRecentThan, int offset,
                                                        int limit) {
        String tableName = findTableName(orderType);

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("date", moreRecentThan);
        parameters.addValue("offset", offset);
        parameters.addValue("limit", limit);

        return namedParameterJdbcTemplate
                .query("SELECT * FROM " + tableName + " WHERE date_placed>=:date LIMIT :limit OFFSET :offset",
                        parameters,
                        orderMapper(orderType));
    }

    private <T extends AbstractOrder> RowMapper<T> orderMapper(Class<T> clazz) {
        return (rs, rowNum) -> {
            UUID uuid = rs.getObject("id", UUID.class);
            LocalDate date = rs.getDate("date_placed").toLocalDate();

            T order;
            try {
                order = clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException();
            }

            order.setId(AbstractOrder.Id.of(uuid, date));

            order.setOrderNumber(rs.getInt("order_number"));
            order.setStatus(ShipmentStatus.valueOf(rs.getString("status")));
            order.setDateUpdated(rs.getDate("date_updated").toLocalDate());
            order.setCustomerId(rs.getObject("customer_id", UUID.class));
            order.setPaymentMethod(rs.getObject("payment_method_id", UUID.class));

            order.setDeliverToFirstName(rs.getString("deliv_to_first_name"));
            order.setDeliverToLastName(rs.getString("deliv_to_last_name"));
            order.setDeliveryAddress((Address.builder()
                    .setAddress1(rs.getString("bill_address1"))
                    .setAddress2(rs.getString("bill_address1"))
                    .setCity(rs.getString("bill_city"))
                    .setCountry(new Country(rs.getString("bill_country_code"), rs.getString("bill_country_name")))
                    .setPostcode(rs.getString("bill_postcode")).build()));

            order.setBillToFirstName(rs.getString(6));
            order.setBillToLastName(rs.getString(6));
            order.setBillAddress(Address.builder()
                    .setAddress1(rs.getString("deliv_address1"))
                    .setAddress2(rs.getString("deliv_address2"))
                    .setCity(rs.getString("deliv_city"))
                    .setCountry(new Country(rs.getString("deliv_country_code"), rs.getString("deliv_country_name")))
                    .setPostcode(rs.getString("deliv_postcode")).build());

            order.setTotalPrice(Money.of(rs.getString("amount"), rs.getString("currency")));

            return order;
        };
    }
}

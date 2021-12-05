package io.roach.workload.orders;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.util.PropertyPlaceholderHelper;

import io.roach.workload.common.util.ResourceSupport;
import io.roach.workload.orders.model.*;

public class SchemaSupport {
    public static final List<Class<? extends AbstractOrder>> orderEntities = new ArrayList<>();

    static {
        orderEntities.add(Order1.class);
        orderEntities.add(Order2.class);
        orderEntities.add(Order3.class);
        orderEntities.add(Order4.class);
        orderEntities.add(Order5.class);
        orderEntities.add(Order6.class);
        orderEntities.add(Order7.class);
        orderEntities.add(Order8.class);
        orderEntities.add(Order9.class);
        orderEntities.add(Order10.class);
        orderEntities.add(Order11.class);
        orderEntities.add(Order12.class);
        orderEntities.add(Order13.class);
        orderEntities.add(Order14.class);
        orderEntities.add(Order15.class);
        orderEntities.add(Order16.class);
        orderEntities.add(Order17.class);
        orderEntities.add(Order18.class);
        orderEntities.add(Order19.class);
        orderEntities.add(Order20.class);
    }

    private final DataSource dataSource;

    private final JdbcTemplate jdbcTemplate;

    public SchemaSupport(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void dropSchema(int partitions) {
        final PropertyPlaceholderHelper plh = new PropertyPlaceholderHelper("${", "}");
        DatabasePopulatorUtils.execute(connection -> IntStream.rangeClosed(1, partitions)
                .forEach(p -> {
                    String sql = plh
                            .replacePlaceholders(ResourceSupport.resourceAsString("db/orders/drop-orders.sql"),
                                    placeholderName -> {
                                        if ("partition".equals(placeholderName)) {
                                            return p + "";
                                        }
                                        return "???";
                                    });
                    jdbcTemplate.execute(sql);
                }), dataSource);
    }

    public void createSchema(int partitions, String countryCode) {
        final PropertyPlaceholderHelper plh = new PropertyPlaceholderHelper("${", "}");
        DatabasePopulatorUtils.execute(connection -> IntStream.rangeClosed(1, partitions)
                .forEach(p -> {
                    String sql = plh
                            .replacePlaceholders(ResourceSupport.resourceAsString("db/orders/create-orders.sql"),
                                    placeholderName -> {
                                        if ("partition".equals(placeholderName)) {
                                            return p + "";
                                        }
                                        if ("country".equals(placeholderName)) {
                                            return countryCode;
                                        }
                                        return "???";
                                    });
                    jdbcTemplate.execute(sql);
                }), dataSource);
    }
}

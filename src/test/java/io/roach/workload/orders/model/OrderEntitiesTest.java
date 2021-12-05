package io.roach.workload.orders.model;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import io.roach.workload.orders.SchemaSupport;

public class OrderEntitiesTest {
    @Test
    public void testCreate() {
        IntStream.rangeClosed(1, 500_000).forEach(value -> {
            OrderEntities.generateOrderEntities(SchemaSupport.orderEntities.get(0), 16,
                    false);
            if (value % 10_000 == 0) {
                System.out.print(".");
            }
        });
    }
}

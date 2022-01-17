package io.roach.workload.orders.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import io.roach.workload.common.util.RandomData;

public class OrderEntities {
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private static final List<UUID> RAND_IDS = new ArrayList<>();

    static {
        IntStream.rangeClosed(1, 100).forEach(value -> RAND_IDS.add(UUID.randomUUID()));
    }

    private OrderEntities() {
    }

    private static final ThreadLocalRandom random = ThreadLocalRandom.current();

    public static <T extends AbstractOrder> List<T> generateOrderEntities(Class<T> orderType,
                                                                          int orderCount) {
        final List<T> orders = new ArrayList<>(orderCount);

        final Address a1 = randomAddress();
        final Address a2 = randomAddress();
        final LocalDate date = LocalDate.now().minusMonths(random.nextInt(3));

        IntStream.rangeClosed(1, orderCount).forEach(value -> {
            T order;
            try {
                order = orderType.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }

            order.setId(AbstractOrder.Id.of(UUID.randomUUID(), date));
            order.setOrderNumber(SEQUENCE.incrementAndGet());
            order.setStatus(RandomData.selectRandom(ShipmentStatus.values()));
            order.setDateUpdated(date);
            order.setCustomerId(RandomData.selectRandom(RAND_IDS));
            order.setPaymentMethod(RandomData.selectRandom(RAND_IDS));
            order.setTotalPrice(RandomData.randomMoneyBetween(50.00, 10_000.00, Currency.getInstance(Locale.US)));

            String fn = RandomData.randomFirstName();
            String ln = RandomData.randomLastName();

            order.setDeliverToFirstName(fn);
            order.setDeliverToLastName(ln);
            order.setDeliveryAddress(a1);

            order.setBillToFirstName(fn);
            order.setBillToLastName(ln);
            order.setBillAddress(a2);

            order.setCustomer(randomCustomer());

            orders.add(order);
        });

        return orders;
    }

    public static Customer randomCustomer() {
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setUserName(RandomData.randomFirstName());
        c.setAddress(randomAddress());
        c.setEmail(RandomData.randomEmail());
        c.setFirstName(RandomData.randomFirstName());
        c.setLastName(RandomData.randomLastName());
        c.setTelephone(RandomData.randomPhoneNumber());
        return c;
    }

    public static Address randomAddress() {
        return Address.builder()
                .setAddress1(RandomData.randomWord(15))
                .setAddress2(RandomData.randomWord(15))
                .setCity(RandomData.randomCity())
                .setPostcode(RandomData.randomZipCode())
                .setCountry(Country.getDefault())
                .build();
    }
}

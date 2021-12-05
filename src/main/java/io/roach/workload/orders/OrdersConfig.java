package io.roach.workload.orders;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import io.roach.workload.Profiles;
import io.roach.workload.common.config.AopConfig;
import io.roach.workload.common.config.JpaConfig;

@Configuration
@Import({JpaConfig.class, AopConfig.class})
@EnableJpaRepositories(basePackages = {"io.roach.workload.orders"})
@Profiles.Orders
public class OrdersConfig {
}

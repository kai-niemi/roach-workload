package io.roach.workload.events;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import io.roach.workload.Profiles;
import io.roach.workload.common.config.JpaConfig;

@Configuration
@Import(JpaConfig.class)
@Profiles.Events
public class EventsConfig {
}

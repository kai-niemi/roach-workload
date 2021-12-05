package io.roach.workload.common.config;

import org.springframework.context.annotation.Bean;

import io.roach.workload.common.aspect.RetryableAspect;
import io.roach.workload.common.aspect.SessionHintsAspect;

public class AopConfig {
    @Bean
    public RetryableAspect retryableAspect() {
        return new RetryableAspect();
    }

    @Bean
    public SessionHintsAspect sessionHintsAspect() {
        return new SessionHintsAspect();
    }
}

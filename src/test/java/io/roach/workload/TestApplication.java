package io.roach.workload;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
})
@ComponentScan
@Configuration
@EnableConfigurationProperties
public class TestApplication {
}

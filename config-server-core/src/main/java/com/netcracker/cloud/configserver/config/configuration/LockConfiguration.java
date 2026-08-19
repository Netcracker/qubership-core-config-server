package com.netcracker.cloud.configserver.config.configuration;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;

import javax.sql.DataSource;
import java.time.Duration;


@Configuration
public class LockConfiguration {

    @Value("${config.server.lock.jdbc.ttl_sec:300}")
    private Long ttlSec;

    @Bean
    public DefaultLockRepository DefaultLockRepository(DataSource dataSource) {
        return new DefaultLockRepository(dataSource);
    }

    @Bean
    public JdbcLockRegistry jdbcLockRegistry(LockRepository lockRepository) {
        return new JdbcLockRegistry(lockRepository, Duration.ofSeconds(ttlSec)); // time to live of a lock record in DB
    }
}

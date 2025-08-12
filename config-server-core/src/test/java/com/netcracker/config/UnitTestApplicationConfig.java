package com.netcracker.config;

import com.netcracker.cloud.configserver.PostgresqlConfiguration;
import com.netcracker.cloud.configserver.config.configuration.ConsulConfiguration;
import com.netcracker.cloud.configserver.config.controller.ConfigPropertiesController;
import com.netcracker.cloud.configserver.config.pojo.ConfigServerConfig;
import com.netcracker.cloud.configserver.config.repository.*;
import com.netcracker.cloud.configserver.config.service.ConsulService;
import com.netcracker.cloud.configserver.encryption.EncryptionService;
import com.netcracker.cloud.configserver.encryption.configuration.NoopEncryptionConfiguration;
import com.netcracker.cloud.configserver.util.TestUtils;

import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.integration.support.locks.DefaultLockRegistry;

@Configuration
@EnableConfigurationProperties(ConfigServerConfig.class)
@Import({PostgresqlConfiguration.class, ConsulConfiguration.class, NoopEncryptionConfiguration.class})
@PropertySource("classpath:application.properties")
public class UnitTestApplicationConfig {

    @Bean
    public ConfigPropertiesController getConfigPropertiesController(@Nonnull EncryptionService encryptionService, ExtendedConfigPropertiesRepository configPropertiesRepository) throws Exception {
        return new ConfigPropertiesController(encryptionService, configPropertiesRepository, new DefaultLockRegistry());
    }

    @Bean
    public DefaultEnvironmentRepository postgresEnvironmentRepository() {
        return new DefaultEnvironmentRepository();
    }

    @Bean
    public ConfigProfileProvider configProfileProvider(ConfigPropertiesRepository repository, @Autowired(required = false) ConsulService consulService) {
        return new ConfigProfileProvider(repository, consulService);
    }

    @Bean
    public TestUtils getTestUtils() {
        return new TestUtils();
    }

    @Bean
    public ExtendedConfigPropertiesRepository extendedConfigPropertiesRepository(ConfigPropertiesRepository jpaConfigPropertiesRepository) {
        return new JpaConfigPropertiesRepository(jpaConfigPropertiesRepository);
    }
}
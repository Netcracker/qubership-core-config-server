package com.netcracker.cloud.configserver.config.configuration;

import com.netcracker.cloud.configserver.config.controller.WhitespaceTolerantEnvironmentController;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.config.server.config.ConfigServerProperties;
import org.springframework.cloud.config.server.encryption.EnvironmentEncryptor;
import org.springframework.cloud.config.server.environment.EnvironmentController;
import org.springframework.cloud.config.server.environment.EnvironmentEncryptorEnvironmentRepository;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Replaces Spring Cloud Config's {@code environmentController} bean
 * ({@code spring.main.allow-bean-definition-overriding=true}).
 */
@Configuration
public class EnvironmentControllerConfiguration {

    @Bean
    @Primary
    public EnvironmentController environmentController(
            EnvironmentRepository envRepository,
            ConfigServerProperties server,
            ObjectProvider<List<EnvironmentEncryptor>> environmentEncryptors,
            ObjectProvider<ObservationRegistry> observationRegistry,
            ObjectProvider<JsonMapper> jsonMapper) {
        EnvironmentEncryptorEnvironmentRepository encrypted = new EnvironmentEncryptorEnvironmentRepository(
                envRepository,
                environmentEncryptors.getIfAvailable(),
                observationRegistry.getIfAvailable(() -> ObservationRegistry.NOOP));
        encrypted.setOverrides(server.getOverrides());

        WhitespaceTolerantEnvironmentController controller = new WhitespaceTolerantEnvironmentController(
                encrypted,
                jsonMapper.getIfAvailable(JsonMapper::new));
        controller.setStripDocumentFromYaml(server.isStripDocumentFromYaml());
        controller.setAcceptEmpty(server.isAcceptEmpty());
        return controller;
    }
}

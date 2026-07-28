package com.netcracker.cloud.configserver.config.configuration;

import com.netcracker.cloud.configserver.config.controller.WhitespaceTolerantEnvironmentController;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.config.server.config.ConfigServerProperties;
import org.springframework.cloud.config.server.encryption.EnvironmentEncryptor;
import org.springframework.cloud.config.server.environment.EnvironmentController;
import org.springframework.cloud.config.server.environment.EnvironmentEncryptorEnvironmentRepository;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Replaces Spring Cloud Config's {@code environmentController} bean.
 * <p>
 * Spring Cloud may re-register its own bean later (RefreshScope). A {@link BeanPostProcessor}
 * ensures whatever {@link EnvironmentController} ends up active has strict validation disabled;
 * {@link WhitespaceTolerantEnvironmentController} still validates a whitespace-normalized copy.
 */
@Configuration
public class EnvironmentControllerConfiguration {

    @Bean
    @Primary
    @RefreshScope
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

    /**
     * Fallback when Spring Cloud's EnvironmentController wins bean-definition overriding:
     * disable PathUtils profile validation so "default, test" does not return 400.
     */
    @Bean
    public static BeanPostProcessor environmentControllerValidationPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof EnvironmentController controller
                        && !(bean instanceof WhitespaceTolerantEnvironmentController)) {
                    controller.setValidateProfiles(false);
                }
                return bean;
            }
        };
    }
}

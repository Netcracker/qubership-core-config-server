package com.netcracker.cloud.configserver.config.configuration;

import com.netcracker.cloud.configserver.config.controller.WhitespaceTolerantEnvironmentController;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.config.ConfigServerProperties;
import org.springframework.cloud.config.server.environment.EnvironmentController;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import tools.jackson.databind.json.JsonMapper;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentControllerConfigurationTest {

    @Test
    void environmentController_isWhitespaceTolerant() {
        EnvironmentRepository repository = (application, profile, label) ->
                new Environment(application, profile == null ? new String[]{"default"} : new String[]{profile});

        EnvironmentControllerConfiguration configuration = new EnvironmentControllerConfiguration();
        EnvironmentController controller = configuration.environmentController(
                repository,
                new ConfigServerProperties(),
                provider(null),
                provider(ObservationRegistry.NOOP),
                provider(new JsonMapper()));

        assertThat(controller).isInstanceOf(WhitespaceTolerantEnvironmentController.class);
        assertThat(controller.getEnvironment("app", "default, test", null, false).getProfiles())
                .containsExactly("default, test");
    }

    @Test
    void postProcessor_disablesValidationOnStockController() {
        EnvironmentController stock = new EnvironmentController(
                (application, profile, label) -> new Environment(application, "default"),
                new JsonMapper());
        // default validateProfiles is true
        Object processed = EnvironmentControllerConfiguration.environmentControllerValidationPostProcessor()
                .postProcessBeforeInitialization(stock, "environmentController");

        assertThat(processed).isSameAs(stock);
        // After BPP, spaced profiles must not be rejected by parent validation
        assertThat(stock.getEnvironment("app", "default, test", null, false).getName()).isEqualTo("app");
    }

    private static <T> ObjectProvider<T> provider(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject(Object... args) {
                return value;
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public T getIfUnique() {
                return value;
            }

            @Override
            public T getObject() {
                return value;
            }

            @Override
            public void ifAvailable(Consumer<T> dependencyConsumer) {
                if (value != null) {
                    dependencyConsumer.accept(value);
                }
            }
        };
    }
}

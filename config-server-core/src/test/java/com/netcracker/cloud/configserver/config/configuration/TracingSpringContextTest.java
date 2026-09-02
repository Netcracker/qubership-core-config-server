package com.netcracker.cloud.configserver.config.configuration;

import io.micrometer.observation.ObservationPredicate;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.boot.micrometer.tracing.autoconfigure.MicrometerTracingAutoConfiguration;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.OpenTelemetryTracingAutoConfiguration;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpTracingAutoConfiguration;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loads the real {@code application.yml} tracing section and verifies that
 * {@code TRACING_ENABLED} gates OTLP export.
 */
class TracingSpringContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> {
                try {
                    List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                            .load("application-yml", new ClassPathResource("application.yml"));
                    for (PropertySource<?> source : sources) {
                        context.getEnvironment().getPropertySources().addLast(source);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to load application.yml", e);
                }
            })
            .withConfiguration(AutoConfigurations.of(
                    ObservationAutoConfiguration.class,
                    OpenTelemetrySdkAutoConfiguration.class,
                    MicrometerTracingAutoConfiguration.class,
                    OpenTelemetryTracingAutoConfiguration.class,
                    OtlpTracingAutoConfiguration.class
            ))
            .withUserConfiguration(TracingConfiguration.class);

    @Test
    void tracingDisabled_doesNotCreateOtlpExporter() {
        contextRunner
                .withPropertyValues("TRACING_ENABLED=false")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).doesNotHaveBean(OtlpHttpSpanExporter.class);
                    assertThat(ctx).hasBean("skipNonBusinessEndpoints");
                });
    }

    @Test
    void tracingEnabled_createsOtlpExporter() {
        contextRunner
                .withPropertyValues("TRACING_ENABLED=true")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(OtlpHttpSpanExporter.class);
                    assertThat(ctx).hasBean("skipNonBusinessEndpoints");

                    ObservationPredicate predicate =
                            ctx.getBean("skipNonBusinessEndpoints", ObservationPredicate.class);
                    assertThat(predicate.test("http.server.requests", contextFor("/api/v1/configs"))).isTrue();
                    assertThat(predicate.test("http.server.requests", contextFor("/readiness"))).isFalse();
                });
    }

    private static ServerRequestObservationContext contextFor(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        return new ServerRequestObservationContext(request, new MockHttpServletResponse());
    }
}

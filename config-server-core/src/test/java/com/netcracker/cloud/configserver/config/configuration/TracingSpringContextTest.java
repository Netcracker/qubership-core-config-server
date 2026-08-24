package com.netcracker.cloud.configserver.config.configuration;

import io.micrometer.observation.ObservationPredicate;
import io.micrometer.tracing.Tracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.boot.micrometer.tracing.autoconfigure.MicrometerTracingAutoConfiguration;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.OpenTelemetryTracingAutoConfiguration;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpTracingAutoConfiguration;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Higher-level Spring context tests: start tracing auto-configuration with
 * {@code TRACING_ENABLED} on/off and verify that management properties and beans
 * match {@code application.yml}.
 */
class TracingSpringContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ObservationAutoConfiguration.class,
                    OpenTelemetrySdkAutoConfiguration.class,
                    MicrometerTracingAutoConfiguration.class,
                    OpenTelemetryTracingAutoConfiguration.class,
                    OtlpTracingAutoConfiguration.class
            ))
            .withUserConfiguration(TracingConfiguration.class);

    @Test
    void tracingEnabled_appliesPlatformPropertiesAndCreatesExporter() {
        contextRunner
                .withPropertyValues(platformTracingProperties(true, "test-collector", "0.25", "test-ns"))
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    Environment env = context.getEnvironment();
                    assertThat(env.getProperty("management.tracing.export.enabled", Boolean.class)).isTrue();
                    assertThat(env.getProperty("management.tracing.sampling.probability", Double.class)).isEqualTo(0.25);
                    assertThat(env.getProperty("management.opentelemetry.tracing.export.otlp.endpoint"))
                            .isEqualTo("http://test-collector:4318/v1/traces");
                    assertThat(env.getProperty("management.opentelemetry.resource-attributes.service.name"))
                            .isEqualTo("config-server-test-ns");

                    assertThat(context).hasBean("skipNonBusinessEndpoints");
                    assertThat(context).hasSingleBean(Tracer.class);
                    assertThat(context).hasSingleBean(OtlpHttpSpanExporter.class);

                    ObservationPredicate predicate =
                            context.getBean("skipNonBusinessEndpoints", ObservationPredicate.class);
                    assertThat(predicate.test("http.server.requests", contextFor("/api/v1/configs"))).isTrue();
                    assertThat(predicate.test("http.server.requests", contextFor("/readiness"))).isFalse();
                    assertThat(predicate.test("http.server.requests", contextFor("/actuator/health"))).isFalse();
                });
    }

    @Test
    void tracingDisabled_keepsExportOffAndDoesNotCreateOtlpExporter() {
        contextRunner
                .withPropertyValues(platformTracingProperties(false, "nc-diagnostic-agent", "0.01", "unknown"))
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    Environment env = context.getEnvironment();
                    assertThat(env.getProperty("management.tracing.export.enabled", Boolean.class)).isFalse();
                    assertThat(env.getProperty("management.tracing.sampling.probability", Double.class)).isEqualTo(0.01);
                    assertThat(env.getProperty("management.opentelemetry.tracing.export.otlp.endpoint"))
                            .isEqualTo("http://nc-diagnostic-agent:4318/v1/traces");

                    assertThat(context).hasBean("skipNonBusinessEndpoints");
                    assertThat(context).doesNotHaveBean(OtlpHttpSpanExporter.class);
                });
    }

    /**
     * Mirrors placeholders from {@code application.yml}:
     * {@code management.tracing.export.enabled=${TRACING_ENABLED:false}} etc.
     * OTLP export is gated only by the global export.enabled flag.
     */
    private static String[] platformTracingProperties(boolean enabled, String host, String probability, String namespace) {
        return new String[]{
                "management.tracing.export.enabled=" + enabled,
                "management.tracing.propagation.produce=B3_MULTI",
                "management.tracing.propagation.consume=B3_MULTI,W3C",
                "management.tracing.sampling.probability=" + probability,
                "management.otlp.metrics.export.enabled=false",
                "management.opentelemetry.resource-attributes.service.name=config-server-" + namespace,
                "management.opentelemetry.tracing.export.otlp.endpoint=http://" + host + ":4318/v1/traces"
        };
    }

    private static ServerRequestObservationContext contextFor(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        return new ServerRequestObservationContext(request, new MockHttpServletResponse());
    }
}

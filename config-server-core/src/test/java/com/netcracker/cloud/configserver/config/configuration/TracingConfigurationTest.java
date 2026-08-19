package com.netcracker.cloud.configserver.config.configuration;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationPredicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TracingConfigurationTest {

    private ObservationPredicate predicate;

    @BeforeEach
    void setUp() {
        predicate = new TracingConfiguration().skipNonBusinessEndpoints();
    }

    @Test
    void allowsNonHttpObservationContext() {
        assertTrue(predicate.test("http.server.requests", new Observation.Context()));
    }

    @Test
    void allowsWhenRequestUriIsNull() {
        MockHttpServletRequest request = mock(MockHttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(null);
        ServerRequestObservationContext context =
                new ServerRequestObservationContext(request, new MockHttpServletResponse());

        assertTrue(predicate.test("http.server.requests", context));
    }

    @Test
    void allowsBusinessEndpoint() {
        assertTrue(predicate.test("http.server.requests", contextFor("/api/v1/configs")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/actuator",
            "/actuator/health",
            "/health",
            "/health/liveness",
            "/liveness",
            "/livez",
            "/readiness",
            "/readyz",
            "/healthz",
            "/metrics",
            "/prometheus",
            "/q/",
            "/q/health"
    })
    void skipsProbeAndManagementEndpoints(String path) {
        assertFalse(predicate.test("http.server.requests", contextFor(path)));
    }

    private static ServerRequestObservationContext contextFor(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        return new ServerRequestObservationContext(request, new MockHttpServletResponse());
    }
}

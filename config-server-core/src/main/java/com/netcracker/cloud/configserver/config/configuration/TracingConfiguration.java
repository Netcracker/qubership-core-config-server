package com.netcracker.cloud.configserver.config.configuration;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@Configuration
public class TracingConfiguration {

    @Bean
    ObservationPredicate skipNonBusinessEndpoints() {
        return (name, context) -> {
            if (!(context instanceof ServerRequestObservationContext serverContext)) {
                return true;
            }
            String path = serverContext.getCarrier().getRequestURI();
            if (path == null) {
                return true;
            }
            return !(path.startsWith("/actuator")
                    || path.startsWith("/health")
                    || path.equals("/liveness")
                    || path.equals("/livez")
                    || path.equals("/readiness")
                    || path.equals("/readyz")
                    || path.equals("/healthz")
                    || path.equals("/metrics")
                    || path.equals("/prometheus")
                    || path.startsWith("/q/"));
        };
    }
}

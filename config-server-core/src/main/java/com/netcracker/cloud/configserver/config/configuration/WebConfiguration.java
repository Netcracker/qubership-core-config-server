package com.netcracker.cloud.configserver.config.configuration;

import com.netcracker.cloud.restclient.MicroserviceRestClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.UrlHandlerFilter;

@Configuration
public class WebConfiguration {

    @Bean
    public UrlHandlerFilter urlHandlerFilter() {
        return UrlHandlerFilter.trailingSlashHandler("/**")
                .wrapRequest()
                .build();
    }

    @Bean("simpleMicroserviceRestClientFactory")
    public MicroserviceRestClientFactory getMicroserviceRestClientFactory(
            @Value("${rest-client.pool.max-idle-time-sec:20}") int maxIdleTime,
            @Value("${rest-client.pool.pending-acquire-timeout-sec:30}") int pendingAcquireTimeout,
            @Value("${rest-client.pool.evict-in-background-sec:120}") int evictInBackground) {
        return new MicroserviceWebClientFactory(maxIdleTime, pendingAcquireTimeout, evictInBackground);
    }
}

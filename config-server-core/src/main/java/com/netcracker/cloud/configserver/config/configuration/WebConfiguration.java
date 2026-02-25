package com.netcracker.cloud.configserver.config.configuration;

import com.netcracker.cloud.restclient.MicroserviceRestClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.UrlHandlerFilter;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.pattern.PathPatternParser;

@Configuration
public class WebConfiguration {

    @Bean
    public FilterRegistrationBean<UrlHandlerFilter> urlHandlerFilter() {
        UrlHandlerFilter filter = UrlHandlerFilter.trailingSlashHandler("/**")
                .redirect(HttpStatus.PERMANENT_REDIRECT)
                .build();
        return new FilterRegistrationBean<>(filter);
    }

    @Bean("simpleMicroserviceRestClientFactory")
    public MicroserviceRestClientFactory getMicroserviceRestClientFactory(
            @Value("${rest-client.pool.max-idle-time-sec:20}") int maxIdleTime,
            @Value("${rest-client.pool.pending-acquire-timeout-sec:30}") int pendingAcquireTimeout,
            @Value("${rest-client.pool.evict-in-background-sec:120}") int evictInBackground) {
        return new MicroserviceWebClientFactory(maxIdleTime, pendingAcquireTimeout, evictInBackground);
    }
}

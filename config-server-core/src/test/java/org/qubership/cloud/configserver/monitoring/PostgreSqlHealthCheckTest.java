package org.qubership.cloud.configserver.monitoring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.ConfigProperty;
import org.qubership.cloud.configserver.config.repository.ConfigPropertiesRepository;
import org.qubership.cloud.configserver.config.repository.DefaultEnvironmentRepository;
import org.springframework.boot.actuate.health.Status;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PostgreSqlHealthCheckTest {

    private static final String TEST = "test";

    @InjectMocks
    private PostgreSqlDbHealthCheck postgreSqlDbHealthCheck;

    @Mock
    private ConfigPropertiesRepository configPropertiesRepository;

    @Test
    public void healthcheck_Ok() {
        ConfigProfile newConfigProfile = new ConfigProfile();
        newConfigProfile.setApplication(DefaultEnvironmentRepository.CONFIG_PROPERTIES_GLOBAL_APPLICATION_NAME);
        newConfigProfile.setProfile(DefaultEnvironmentRepository.CONFIG_PROPERTIES_DEFAULT_PROFILE_NAME);
        newConfigProfile.setPropertiesFromMap(Collections.singletonMap(TEST, new ConfigProperty(TEST, TEST, false)));
        when(configPropertiesRepository.findByApplicationAndProfile(DefaultEnvironmentRepository.CONFIG_PROPERTIES_GLOBAL_APPLICATION_NAME,
                DefaultEnvironmentRepository.CONFIG_PROPERTIES_DEFAULT_PROFILE_NAME))
                .thenReturn(List.of(newConfigProfile));
        assertEquals(Status.UP, postgreSqlDbHealthCheck.health().getStatus());
    }

    @Test
    public void healthcheck_Problem() {
        when(configPropertiesRepository.findByApplicationAndProfile(anyString(), anyString()))
                .thenThrow(new DataAccessResourceFailureException(TEST));
        assertEquals(HealthCheckStatus.PROBLEM.name(), postgreSqlDbHealthCheck.health().getStatus().toString());
    }

    @Test
    public void healthcheck_Fatal() {
        when(configPropertiesRepository.findByApplicationAndProfile(DefaultEnvironmentRepository.CONFIG_PROPERTIES_GLOBAL_APPLICATION_NAME,
                DefaultEnvironmentRepository.CONFIG_PROPERTIES_DEFAULT_PROFILE_NAME))
                .thenReturn(null);
        assertEquals(Status.OUT_OF_SERVICE, postgreSqlDbHealthCheck.health().getStatus());
    }
}
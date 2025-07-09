package org.qubership.cloud.configserver.config.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.ConfigProperty;
import org.qubership.cloud.configserver.config.repository.ConfigPropertiesRepository;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.qubership.cloud.configserver.config.service.ConsulMigrationValidator.CONSUL_VALUE_SIZE_RESTRICTION_UPPER_LIMIT_BYTES;

@ExtendWith(SpringExtension.class)
class ConsulMigrationServiceTest {
    private final static String NAMESPACE = "test-namespace";
    private static final String MIGRATED_Q = "select m from consul_migrated order by m limit 1";

    private ConsulMigrationService consulMigrationService;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private ConfigPropertiesRepository pgRepository;

    private final ConsulService consulService = spy(new ConsulService(null, NAMESPACE));

    @BeforeEach
    void setUp() {
        this.consulMigrationService = new ConsulMigrationService(NAMESPACE, pgRepository, jdbcTemplate, consulService);
        when(consulService.isConsulAvailable()).thenReturn(true);
    }

    @Test
    void shouldNotMigrateIfAlreadyMigrated() {
        when(jdbcTemplate.queryForObject(MIGRATED_Q, Boolean.class)).thenReturn(true);

        consulMigrationService.migrateToConsul();
        verify(consulService, times(0)).addConfigProfile(any());
    }

    @Test
    void shouldNotMigrateIfNothingToMigrate() {
        when(jdbcTemplate.queryForObject(MIGRATED_Q, Boolean.class)).thenReturn(false);
        when(pgRepository.findAll()).thenReturn(Collections.emptyList());

        consulMigrationService.migrateToConsul();
        verify(consulService, times(0)).addConfigProfile(any());
    }

    @Test
    void shouldSetWdAfterMigration() {
        when(jdbcTemplate.queryForObject(MIGRATED_Q, Boolean.class)).thenReturn(false);

        consulMigrationService.migrateToConsul();
        verify(jdbcTemplate, times(1)).update("update consul_migrated set m = true");
    }

    @Test
    void shouldMigratePGProperties() {
        List<ConfigProperty> properties = new ArrayList<>(2);
        properties.add(new ConfigProperty(UUID.randomUUID(), "tk0", "tv0", false));
        properties.add(new ConfigProperty(UUID.randomUUID(), "tk1", "tv1", false));
        List<ConfigProfile> profiles = new ArrayList<>(1);
        profiles.add(new ConfigProfile(UUID.randomUUID(), "test-app", "test-prof", 1, properties));

        when(pgRepository.findAll()).thenReturn(profiles);
        when(jdbcTemplate.queryForObject(MIGRATED_Q, Boolean.class)).thenReturn(false);
        doNothing().when(consulService).addConfigProfile(any());

        consulMigrationService.migrateToConsul();
        verify(consulService, times(1)).addConfigProfile(profiles.get(0));
    }

    @Test
    void shouldThrowExceptionIfThereIsBigValueInProperties() {
        List<ConfigProperty> properties = new ArrayList<>(2);
        properties.add(new ConfigProperty(UUID.randomUUID(), "tk0", "tv0", false));
        properties.add(new ConfigProperty(UUID.randomUUID(), "tk1", "tv1", false));

        String bigValue = RandomStringUtils.random(CONSUL_VALUE_SIZE_RESTRICTION_UPPER_LIMIT_BYTES * 2);
        properties.add(new ConfigProperty(UUID.randomUUID(), "tk2", bigValue, false));

        List<ConfigProfile> profiles = new ArrayList<>(1);
        profiles.add(new ConfigProfile(UUID.randomUUID(), "test-app", "test-prof", 1, properties));

        when(pgRepository.findAll()).thenReturn(profiles);
        when(jdbcTemplate.queryForObject(MIGRATED_Q, Boolean.class)).thenReturn(false);

        ConsulMigrationException error = Assertions.assertThrowsExactly(ConsulMigrationException.class,
                () -> consulMigrationService.migrateToConsul());
        Assertions.assertTrue(error.getMessage().contains("tk2"));
    }
}
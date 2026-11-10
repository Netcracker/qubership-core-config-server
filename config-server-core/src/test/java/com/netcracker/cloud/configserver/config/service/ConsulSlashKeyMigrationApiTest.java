package com.netcracker.cloud.configserver.config.service;

import com.google.common.net.HostAndPort;
import com.netcracker.cloud.configserver.config.controller.ConfigPropertiesController;
import com.netcracker.cloud.configserver.config.repository.ConsulConfigPropertiesRepository;
import com.netcracker.cloud.configserver.encryption.EncryptionService;
import com.orbitz.consul.Consul;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Migration of entries written by previous versions, as seen through the HTTP API.
 * {@link ConfigPropertiesController} rewrites the whole profile on every save, so any
 * property save migrates every legacy slash key in that profile.
 * See also {@link ConsulSlashKeyProblemsTest} and {@link ConsulSlashKeyLifecycleTest}.
 */
class ConsulSlashKeyMigrationApiTest {

    private static final GenericContainer<?> CONSUL = new GenericContainer<>("hashicorp/consul:1.8.9")
            .withCommand("agent", "-dev", "-client", "0.0.0.0")
            .withExposedPorts(8500)
            .waitingFor(new HostPortWaitStrategy().forPorts(8500));

    private static Consul client;
    private static ConfigPropertiesController controller;

    @BeforeAll
    static void startConsul() {
        CONSUL.start();
        client = Consul.builder()
                .withHostAndPort(HostAndPort.fromParts(CONSUL.getHost(), CONSUL.getFirstMappedPort()))
                .build();
        controller = new ConfigPropertiesController(
                mock(EncryptionService.class),
                new ConsulConfigPropertiesRepository(new ConsulService(client, "ns")),
                new DefaultLockRegistry());
    }

    @AfterAll
    static void stopConsul() {
        CONSUL.stop();
    }

    private static Map<String, String> consulEntries(String app) {
        Map<String, String> entries = new TreeMap<>();
        client.keyValueClient().getValues("config/ns/" + app + "/")
                .forEach(v -> entries.put(v.getKey(), v.getValueAsString().orElse("")));
        return entries;
    }

    @Test
    void savingAnotherPropertyThroughTheApiMigratesEveryLegacySlashKeyInTheProfile() throws Exception {
        client.keyValueClient().putValue(
                "config/ns/migrate-app/logging&sol;level&sol;org/apache/kafka/clients/consumer/ConsumerConfig", "DEBUG");

        assertEquals(201, controller.addProperties("migrate-app", "default", Map.of("foo", "bar")).getStatusCode().value());

        assertEquals(Map.of(
                "config/ns/migrate-app/foo", "bar",
                "config/ns/migrate-app/logging/level/org.apache.kafka.clients.consumer.ConsumerConfig", "DEBUG"
        ), consulEntries("migrate-app"));
    }
}

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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ConsulSlashKeyProblemsTest {

    private static final String SLASH_KEY = "logging/level/org.apache.kafka.clients.consumer.ConsumerConfig";

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
    void updatingASlashKeyLeavesOnlyTheNewValueInConsul() throws Exception {
        controller.addProperties("update-app", "default", Map.of(SLASH_KEY, "DEBUG"));
        controller.addProperties("update-app", "default", Map.of("foo", "bar"));

        controller.addProperties("update-app", "default", Map.of(SLASH_KEY, "INFO"));

        Map<String, String> entries = consulEntries("update-app");
        assertEquals(List.of("INFO", "bar"), entries.values().stream().sorted().collect(Collectors.toList()),
                entries.toString());
    }

    @Test
    void deletingASlashKeyRemovesItFromConsul() throws Exception {
        controller.addProperties("delete-app", "default", Map.of(SLASH_KEY, "DEBUG"));
        controller.addProperties("delete-app", "default", Map.of("foo", "bar"));

        controller.deleteProperties("delete-app", "default", List.of(SLASH_KEY));

        assertEquals(Map.of("config/ns/delete-app/foo", "bar"), consulEntries("delete-app"));
    }

    @Test
    void aProfileWithParentAndChildLoggerSlashKeysStaysWritable() throws Exception {
        controller.addProperties("logger-app", "default", Map.of("logging/level/com.example", "DEBUG"));
        controller.addProperties("logger-app", "default", Map.of("logging/level/com.example.sub", "INFO"));

        assertEquals(201, controller.addProperties("logger-app", "default", Map.of("foo", "bar")).getStatusCode().value());
    }

    @Test
    void deletingASlashKeyRemovesTheEntryWrittenByThePreviousVersion() throws Exception {
        client.keyValueClient().putValue(
                "config/ns/legacy-app/logging&sol;level&sol;org/apache/kafka/clients/consumer/ConsumerConfig", "DEBUG");

        controller.deleteProperties("legacy-app", "default", List.of(SLASH_KEY));

        assertEquals(Map.of(), consulEntries("legacy-app"));
    }
}

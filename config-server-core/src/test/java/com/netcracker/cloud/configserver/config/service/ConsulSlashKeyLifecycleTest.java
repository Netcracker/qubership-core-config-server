package com.netcracker.cloud.configserver.config.service;

import com.google.common.net.HostAndPort;
import com.netcracker.cloud.configserver.config.ConfigProfile;
import com.netcracker.cloud.configserver.config.ConfigProperty;
import com.orbitz.consul.Consul;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Contract for keys containing '/': one API key name maps to exactly one Consul path
 * for write, read, update and delete, and entries written by previous versions
 * ({@code &sol;} encoding) are found, updated (migrated) and deleted by their original name.
 */
class ConsulSlashKeyLifecycleTest {

    private static final String FQCN_KEY = "logging/level/org.apache.kafka.clients.consumer.ConsumerConfig";
    private static final String FQCN_PATH = "logging/level/org.apache.kafka.clients.consumer.ConsumerConfig";
    // what the previous version stored for FQCN_KEY
    private static final String LEGACY_PATH = "logging&sol;level&sol;org/apache/kafka/clients/consumer/ConsumerConfig";
    private static final String NO_DOT_KEY = "logging/level/root";
    private static final String NO_DOT_LEGACY_PATH = "logging&sol;level&sol;root";

    private static final GenericContainer<?> CONSUL = new GenericContainer<>("hashicorp/consul:1.8.9")
            .withCommand("agent", "-dev", "-client", "0.0.0.0")
            .withExposedPorts(8500)
            .waitingFor(new HostPortWaitStrategy().forPorts(8500));

    private static Consul client;
    private static ConsulService service;
    private static int appCounter;

    @BeforeAll
    static void start() {
        CONSUL.start();
        client = Consul.builder()
                .withHostAndPort(HostAndPort.fromParts(CONSUL.getHost(), CONSUL.getFirstMappedPort()))
                .build();
        service = new ConsulService(client, "ns");
    }

    @AfterAll
    static void stop() {
        CONSUL.stop();
    }

    private static String newApp() {
        return "lifecycle-app-" + (++appCounter);
    }

    private static ConfigProfile profile(String app, ConfigProperty... properties) {
        ConfigProfile profile = new ConfigProfile();
        profile.setApplication(app);
        profile.setProfile("default");
        profile.setProperties(List.of(properties));
        return profile;
    }

    private static ConfigProperty prop(String key, String value) {
        return new ConfigProperty(key, value, false);
    }

    /** Raw Consul content relative to the application prefix. */
    private static Map<String, String> stored(String app) {
        String prefix = "config/ns/" + app + "/";
        Map<String, String> entries = new TreeMap<>();
        client.keyValueClient().getValues(prefix)
                .forEach(v -> entries.put(v.getKey().substring(prefix.length()), v.getValueAsString().orElse("")));
        return entries;
    }

    /** What the API returns: original key name -> value. */
    private static Map<String, String> read(String app) {
        Map<String, String> result = new TreeMap<>();
        service.getByApplicationAndProfile(app, "default").getProperties()
                .forEach(p -> result.put(p.getKey(), p.getValue()));
        return result;
    }

    private static void putLegacy(String app, String path, String value) {
        client.keyValueClient().putValue("config/ns/" + app + "/" + path, value);
    }

    @Test
    void newSlashKeyIsWrittenReadUpdatedAndDeletedUnderOneName() {
        String app = newApp();

        service.addConfigProfile(profile(app, prop(FQCN_KEY, "DEBUG"), prop("foo", "bar")));
        assertEquals(Map.of(FQCN_PATH, "DEBUG", "foo", "bar"), stored(app));
        assertEquals(Map.of(FQCN_KEY, "DEBUG", "foo", "bar"), read(app));

        service.addConfigProfile(profile(app, prop(FQCN_KEY, "INFO")));
        assertEquals(Map.of(FQCN_PATH, "INFO", "foo", "bar"), stored(app));
        assertEquals(Map.of(FQCN_KEY, "INFO", "foo", "bar"), read(app));

        service.deleteProperties(profile(app), List.of(FQCN_KEY));
        assertEquals(Map.of("foo", "bar"), stored(app));
        assertEquals(Map.of("foo", "bar"), read(app));
    }

    @Test
    void legacyEntryIsListedUnderItsOriginalName() {
        String app = newApp();
        putLegacy(app, LEGACY_PATH, "DEBUG");

        assertEquals(Map.of(FQCN_KEY, "DEBUG"), read(app));
    }

    @Test
    void updatingALegacyEntryMigratesItAndLeavesOneCopy() {
        String app = newApp();
        putLegacy(app, LEGACY_PATH, "DEBUG");
        putLegacy(app, "foo", "bar");

        service.addConfigProfile(profile(app, prop(FQCN_KEY, "INFO")));

        assertEquals(Map.of(FQCN_PATH, "INFO", "foo", "bar"), stored(app));
        assertEquals(Map.of(FQCN_KEY, "INFO", "foo", "bar"), read(app));
    }

    @Test
    void savingOtherPropertiesDoesNotDuplicateALegacyEntry() {
        String app = newApp();
        putLegacy(app, LEGACY_PATH, "DEBUG");

        service.addConfigProfile(profile(app, prop("foo", "bar")));
        // the API still lists the untouched legacy entry exactly once, under its original name
        assertEquals(Map.of(FQCN_KEY, "DEBUG", "foo", "bar"), read(app));
        assertEquals(Map.of(LEGACY_PATH, "DEBUG", "foo", "bar"), stored(app));
    }

    @Test
    void deletingALegacyEntryByItsOriginalNameRemovesIt() {
        String app = newApp();
        putLegacy(app, LEGACY_PATH, "DEBUG");
        putLegacy(app, "foo", "bar");

        service.deleteProperties(profile(app), List.of(FQCN_KEY));

        assertEquals(Map.of("foo", "bar"), stored(app));
        assertEquals(Map.of("foo", "bar"), read(app));
    }

    @Test
    void deletingRemovesBothTheNewAndTheLegacyCopy() {
        String app = newApp();
        putLegacy(app, LEGACY_PATH, "DEBUG");
        putLegacy(app, FQCN_PATH, "INFO");

        service.deleteProperties(profile(app), List.of(FQCN_KEY));

        assertEquals(Map.of(), stored(app));
    }

    @Test
    void slashKeyWithoutDotsKeepsItsLegacyPathAndNeverClashesWithTheDottedKey() {
        String app = newApp();
        putLegacy(app, NO_DOT_LEGACY_PATH, "DEBUG");

        service.addConfigProfile(profile(app, prop(NO_DOT_KEY, "INFO"), prop("logging.level.root", "WARN")));

        assertEquals(Map.of(NO_DOT_LEGACY_PATH, "INFO", "logging/level/root", "WARN"), stored(app));
        assertEquals(Map.of(NO_DOT_KEY, "INFO", "logging.level.root", "WARN"), read(app));

        service.deleteProperties(profile(app), List.of(NO_DOT_KEY));
        assertEquals(Map.of("logging.level.root", "WARN"), read(app));
    }

    @Test
    void parentAndChildLoggerKeysCanBeSavedInSeparateRequests() {
        String app = newApp();

        service.addConfigProfile(profile(app, prop("logging/level/com.example", "DEBUG")));
        service.addConfigProfile(profile(app, prop("logging/level/com.example.sub", "INFO")));
        service.addConfigProfile(profile(app, prop("foo", "bar")));

        assertEquals(Map.of(
                "logging/level/com.example", "DEBUG",
                "logging/level/com.example.sub", "INFO",
                "foo", "bar"), read(app));
    }
}

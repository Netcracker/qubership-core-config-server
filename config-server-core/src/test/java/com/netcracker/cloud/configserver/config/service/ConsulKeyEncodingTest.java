package com.netcracker.cloud.configserver.config.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ConsulKeyEncodingTest {

    private final ConsulService consulService = new ConsulService(null, "ns");

    @Test
    void dottedSpringKeysMapDotsToConsulFolders() {
        assertEquals("my/lovely/key", consulService.toConsulRelativeKey("my.lovely.key"));
        assertEquals("logging/level/org/apache/kafka/clients/consumer/ConsumerConfig",
                consulService.toConsulRelativeKey("logging.level.org.apache.kafka.clients.consumer.ConsumerConfig"));
    }

    @Test
    void slashClasspathKeysKeepSlashesAndFqcnDots() {
        String consulKey = consulService.toConsulRelativeKey(
                "logging/level/org.apache.kafka.clients.consumer.ConsumerConfig");
        assertEquals("logging/level/org.apache.kafka.clients.consumer.ConsumerConfig", consulKey);
        assertFalse(consulKey.contains("&sol;"));
        assertFalse(consulKey.contains("org/apache/kafka"));
    }

    @Test
    void slashKeysWithFqcnStillEscapeSpecialCharactersExceptSlash() {
        assertEquals("logging/level/foo.bar&ast;baz",
                consulService.toConsulRelativeKey("logging/level/foo.bar*baz"));
        assertEquals("logging/level/foo.bar&quest;baz",
                consulService.toConsulRelativeKey("logging/level/foo.bar?baz"));
        assertEquals("logging/level/foo.bar&apos;baz",
                consulService.toConsulRelativeKey("logging/level/foo.bar'baz"));
        assertEquals("logging/level/foo.bar&percnt;",
                consulService.toConsulRelativeKey("logging/level/foo.bar%"));
    }

    @Test
    void quotedDottedLogCategoryKeysKeepHistoricalFolderMapping() {
        assertEquals("log/category/\"quoted/text\"/level",
                consulService.toConsulRelativeKey("log.category.\"quoted.text\".level"));
        assertEquals("log/category/&apos;apostrophed/text&apos;/level",
                consulService.toConsulRelativeKey("log.category.'apostrophed.text'.level"));
        assertEquals("log/category/&ast;stared/text&ast;/level",
                consulService.toConsulRelativeKey("log.category.*stared.text*.level"));
    }

    @Test
    void everyKeyRoundTripsAndNoTwoKeysShareAConsulPath() {
        java.util.List<String> keys = java.util.List.of(
                "db.url", "a.b", "a/b", "a/b/c", "logging/level/root", "logging.level.root",
                "logging/level/org.apache.kafka.X", "logging/level/com.example", "logging/level/com.example.sub",
                "a.b/c", "a/b.c", "log.category.*stared.text*.level", "x/y.*z");
        java.util.Map<String, String> pathToKey = new java.util.HashMap<>();
        for (String key : keys) {
            String path = consulService.toConsulRelativeKey(key);
            assertEquals(key, consulService.fromConsulRelativeKey(path), "round trip of " + key);
            assertEquals(key, consulService.fromConsulRelativeKey(consulService.legacyToConsulRelativeKey(key)),
                    "legacy round trip of " + key);
            String previous = pathToKey.put(path, key);
            org.junit.jupiter.api.Assertions.assertNull(previous, key + " collides with " + previous + " at " + path);
        }
    }

    @Test
    void slashKeyWithoutDotsKeepsLegacyEncoding() {
        assertEquals("logging&sol;level&sol;root", consulService.toConsulRelativeKey("logging/level/root"));
    }

    @Test
    void readReturnsTheSameApiNameThatWasWritten() {
        assertEquals("my.lovely.key",
                consulService.fromConsulRelativeKey(consulService.toConsulRelativeKey("my.lovely.key")));
        String slashKey = "logging/level/org.apache.kafka.clients.consumer.ConsumerConfig";
        assertEquals(slashKey, consulService.fromConsulRelativeKey(consulService.toConsulRelativeKey(slashKey)));
        assertEquals(slashKey, consulService.fromConsulRelativeKey(
                consulService.legacyToConsulRelativeKey(slashKey)));
    }
}

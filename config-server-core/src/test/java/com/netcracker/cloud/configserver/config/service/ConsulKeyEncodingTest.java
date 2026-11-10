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
    void slashKeysStillEscapeSpecialCharactersExceptSlash() {
        assertEquals("logging/level/foo&ast;bar", consulService.toConsulRelativeKey("logging/level/foo*bar"));
        assertEquals("logging/level/foo&quest;bar", consulService.toConsulRelativeKey("logging/level/foo?bar"));
        assertEquals("logging/level/foo&apos;bar", consulService.toConsulRelativeKey("logging/level/foo'bar"));
        assertEquals("logging/level/100&percnt;", consulService.toConsulRelativeKey("logging/level/100%"));
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
}

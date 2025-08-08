package com.netcracker.cloud.configserver.config.configuration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static com.netcracker.cloud.configserver.config.configuration.DbaasConfigurationBuilder.DB_CLASSIFIER;

@ExtendWith(MockitoExtension.class)
class DbaasConfigurationBuilderTest {

    private static final String MICROSERVICE_NAME = "config-server";
    private static final String LOCAL_DEV_NAMESPACE_ENV_KEY = "LOCALDEV_NAMESPACE";
    private static final String ATTACH_TO_CLOUD_DB_ENV_KEY = "attachToCloudDB";
    private static final String LOCAL_DEV_NAMESPACE = "localDevNamespace";

    @Mock
    private Environment environment;

    private final Map<String, String> properties = new HashMap<>();

    @BeforeEach
    void setUp() {
        properties.put("spring.application.name", MICROSERVICE_NAME);
        properties.put(LOCAL_DEV_NAMESPACE_ENV_KEY, LOCAL_DEV_NAMESPACE);
        when(environment.getProperty(anyString())).thenAnswer((Answer<String>) invocationOnMock -> {
            String propertyKey = invocationOnMock.getArgument(0);
            return properties.get(propertyKey);
        });
    }

    @AfterEach
    void tearDown() {
        reset(environment);
        properties.clear();
    }

    @Test
    void testCreateClassifier_withTenantId() {
        DbaasConfigurationBuilder dbaasConfigurationBuilder = new DbaasConfigurationBuilder(environment);

        Map<String, Object> classifier = dbaasConfigurationBuilder.createClassifier(null);

        assertEquals(MICROSERVICE_NAME, classifier.get("microserviceName"));
        assertEquals("service", classifier.get("scope"));
        assertEquals(DB_CLASSIFIER, classifier.get("dbClassifier"));
        assertEquals(LOCAL_DEV_NAMESPACE, classifier.get("localdev"));
    }

    @Test
    void testCreateClassifier_withoutTenantId() {
        DbaasConfigurationBuilder dbaasConfigurationBuilder = new DbaasConfigurationBuilder(environment);
        String tenantId = UUID.randomUUID().toString();

        Map<String, Object> classifier = dbaasConfigurationBuilder.createClassifier(tenantId);

        assertEquals(MICROSERVICE_NAME, classifier.get("microserviceName"));
        assertEquals(tenantId, classifier.get("tenantId"));
        assertEquals(DB_CLASSIFIER, classifier.get("dbClassifier"));
        assertEquals(LOCAL_DEV_NAMESPACE, classifier.get("localdev"));
    }

    @Test
    void testCreateClassifier_inCloudDbAttachMode() {
        properties.put(ATTACH_TO_CLOUD_DB_ENV_KEY, "true");
        DbaasConfigurationBuilder dbaasConfigurationBuilder = new DbaasConfigurationBuilder(environment);

        Map<String, Object> classifier = dbaasConfigurationBuilder.createClassifier(null);

        assertNull(classifier.get("localdev"));
    }
}

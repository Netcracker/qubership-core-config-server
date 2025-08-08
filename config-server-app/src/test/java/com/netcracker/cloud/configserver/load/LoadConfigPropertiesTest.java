package com.netcracker.cloud.configserver.load;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.configserver.Application;
import org.qubership.cloud.configserver.load.config.JpaConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(
        classes = {
                Application.class,
                JpaConfiguration.class
        }
)
class LoadConfigPropertiesTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ConfigurableEnvironment environment;

    @BeforeEach
    void setup() {
        MutablePropertySources propertySources = environment.getPropertySources();
        Map<String, Object> properties = new HashMap<>();
        propertySources.addFirst(new MapPropertySource("custom properties", properties));
    }

    @Test
    @Repeat(value = 10)
    public void test50Properties10Times() throws Exception {
        mockMvc.perform(post("/app/prof")
                        .content("{\"initial\":\"value\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        Map<String, String> props = new ConcurrentHashMap<>();
        props.put("initial", "value");
        int propNum = 50;
        int totalPropNum = propNum + 1;
        try (ExecutorService threads = Executors.newFixedThreadPool(propNum)) {
            threads.invokeAll(Stream.generate(() -> (Callable<Object>) () -> {
                                long threadId = Thread.currentThread().threadId();
                                String key = "key" + threadId;
                                String value = "value" + threadId;
                                props.put(key, value);
                                mockMvc.perform(post("/app/prof")
                                                .content("{\"" + key + "\":\"" + value + "\"}")
                                                .contentType(MediaType.APPLICATION_JSON))
                                        .andExpect(status().isCreated());
                                return null;
                            }).limit(propNum)
                            .collect(Collectors.toList())
            ).forEach((future) -> {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        Assertions.assertEquals(totalPropNum, props.size());
    }
}

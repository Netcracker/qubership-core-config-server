package com.netcracker.cloud.configserver;

import org.junit.jupiter.api.Test;
import com.netcracker.cloud.configserver.config.SpringUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertSame;

class SpringUtilityTest {

    @Autowired
    private static ApplicationContext applicationContext;

    @Test
    void applicationContextActions() {
        SpringUtility.setApplicationContext(applicationContext);
        assertSame(applicationContext, SpringUtility.getContext());
    }
}

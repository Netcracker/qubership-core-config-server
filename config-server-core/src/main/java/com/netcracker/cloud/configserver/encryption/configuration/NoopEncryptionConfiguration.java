package com.netcracker.cloud.configserver.encryption.configuration;

import com.netcracker.cloud.configserver.encryption.EncryptionService;
import com.netcracker.cloud.configserver.encryption.service.EncryptionServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NoopEncryptionConfiguration {

    @Bean
    public EncryptionService encryptionService() {
        return new EncryptionServiceImpl();
    }
}

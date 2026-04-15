package com.sait.peelin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class SystemUserConfig {

    public static final UUID SYSTEM_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Bean(name = "systemUserId")
    public UUID systemUserId() {
        return SYSTEM_USER_ID;
    }
}

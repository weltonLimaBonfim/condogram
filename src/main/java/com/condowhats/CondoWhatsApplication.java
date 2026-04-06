package com.condowhats;

import com.condowhats.adapter.in.messaging.telegram.TelegramProperties;
import com.condowhats.infrastructure.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({JwtProperties.class, TelegramProperties.class})
public class CondoWhatsApplication {
    public static void main(String[] args) {
        SpringApplication.run(CondoWhatsApplication.class, args);
    }
}

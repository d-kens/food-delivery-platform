package com.example.notification_service.novu;

import co.novu.Novu;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NovuConfig {
    @Bean
    public Novu novu(@Value("${novu.api.key}") String apiKey) {
        return Novu.builder().secretKey(apiKey).build();
    }
}

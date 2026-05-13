package com.clientes_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AbacatePayRestConfig {

    @Bean
    public RestClient abacatePayRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.abacatepay.com")
                .build();
    }
}

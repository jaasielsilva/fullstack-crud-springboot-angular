package com.clientes_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MercadoPagoRestConfig {

    @Bean
    public RestClient mercadoPagoRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.mercadopago.com")
                .build();
    }
}

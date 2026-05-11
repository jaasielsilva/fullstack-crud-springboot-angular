package com.clientes_api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4 com {@code spring-boot-starter-webmvc} não registra {@link ObjectMapper}
 * (Jackson 2) por padrão; vários beans injetam {@code ObjectMapper} diretamente.
 * Este bean garante o carregamento do contexto em testes (H2) e em qualquer perfil sem Jackson MVC.
 */
@Configuration
public class JacksonObjectMapperConfiguration {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

package com.clientes_api.config;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Centraliza regra de verbosidade de logs de segurança: stack traces e detalhes sensíveis
 * apenas fora de perfis de produção.
 */
@Component
public class SecurityLoggingSupport {

    private final Environment environment;

    public SecurityLoggingSupport(Environment environment) {
        this.environment = environment;
    }

    public boolean isVerboseAuthErrorLogging() {
        return !environment.matchesProfiles("prod", "production");
    }
}

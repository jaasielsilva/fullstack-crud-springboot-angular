package com.clientes_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Execução assíncrona (ex.: pós-commit do webhook Mercado Pago) com pool dedicado.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "mercadoPagoWebhookExecutor")
    public Executor mercadoPagoWebhookExecutor(
            @Value("${mercadopago.webhook.async.core-pool-size:2}") int corePoolSize,
            @Value("${mercadopago.webhook.async.max-pool-size:8}") int maxPoolSize,
            @Value("${mercadopago.webhook.async.queue-capacity:200}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setThreadNamePrefix("mp-webhook-");
        ex.setCorePoolSize(corePoolSize);
        ex.setMaxPoolSize(maxPoolSize);
        ex.setQueueCapacity(queueCapacity);
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(30);
        ex.initialize();
        return ex;
    }
}

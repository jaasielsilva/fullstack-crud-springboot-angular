package com.clientes_api.monitoring;

/**
 * Exceção checked usada apenas pelo endpoint de teste de monitoring em HML.
 * Não é tratada pelo {@link com.clientes_api.exception.GlobalExceptionHandler},
 * resultando em HTTP 500 para alimentar métricas Prometheus (status 5xx).
 */
public class MonitoringTestSimulatedFailureException extends Exception {

    public MonitoringTestSimulatedFailureException() {
        super("Simulated 5xx for monitoring test (HML only)");
    }
}

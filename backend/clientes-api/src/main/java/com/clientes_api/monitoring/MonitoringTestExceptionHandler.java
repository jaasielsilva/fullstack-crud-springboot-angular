package com.clientes_api.monitoring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.clientes_api.monitoring.controller.MonitoringTestController;

import java.util.Map;

/**
 * Retorna HTTP 500 diretamente, sem forward para {@code /error} (que o Spring Security bloquearia com 403).
 */
@RestControllerAdvice(assignableTypes = MonitoringTestController.class)
@Profile("hml")
@ConditionalOnProperty(name = "monitoring.test-endpoints.enabled", havingValue = "true")
public class MonitoringTestExceptionHandler {

    @ExceptionHandler(MonitoringTestSimulatedFailureException.class)
    public ResponseEntity<Map<String, String>> handleSimulatedFailure() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error"));
    }
}

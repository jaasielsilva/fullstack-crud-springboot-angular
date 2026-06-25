package com.clientes_api.monitoring.controller;

import com.clientes_api.monitoring.MonitoringTestSimulatedFailureException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints internos para simular taxa de erro 5xx no monitoring (Prometheus/Micrometer).
 * <p>
 * <strong>Uso exclusivo em HML.</strong> Ativo apenas com profile {@code hml} e
 * {@code monitoring.test-endpoints.enabled=true}. Em produção o controller não é carregado.
 * </p>
 * <p>
 * O SaaS Monitor calcula taxa de erros com:
 * {@code 100 × rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(...)}.
 * Rotas {@code /actuator/*} são ignoradas — estes endpoints usam prefixo {@code /api/internal/}.
 * </p>
 * <h3>Teste após deploy (rede Docker na VPS)</h3>
 * <pre>{@code
 * NET=$(docker inspect api-hml --format '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}')
 * SECRET="valor-do-MONITORING_TEST_SECRET"
 *
 * curl -H "X-Monitoring-Test-Secret: $SECRET" http://api-hml:8080/api/internal/monitoring-test/ok
 * curl -H "X-Monitoring-Test-Secret: $SECRET" http://api-hml:8080/api/internal/monitoring-test/error
 *
 * # ~25% erro: 1 erro + 3 OK por ciclo
 * while true; do
 *   docker run --rm --network "$NET" curlimages/curl:8.5.0 -s -o /dev/null -w "err:%{http_code}\n" \
 *     -H "X-Monitoring-Test-Secret: $SECRET" \
 *     http://api-hml:8080/api/internal/monitoring-test/error
 *   for i in 1 2 3; do
 *     docker run --rm --network "$NET" curlimages/curl:8.5.0 -s -o /dev/null -w "ok:%{http_code}\n" \
 *       -H "X-Monitoring-Test-Secret: $SECRET" \
 *       http://api-hml:8080/api/internal/monitoring-test/ok
 *   done
 *   sleep 1
 * done
 * }</pre>
 */
@RestController
@Profile("hml")
@ConditionalOnProperty(name = "monitoring.test-endpoints.enabled", havingValue = "true")
@RequestMapping("/api/internal/monitoring-test")
public class MonitoringTestController {

    @GetMapping("/ok")
    public ResponseEntity<Map<String, String>> ok() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/error")
    public ResponseEntity<Map<String, String>> error() throws MonitoringTestSimulatedFailureException {
        throw new MonitoringTestSimulatedFailureException();
    }
}

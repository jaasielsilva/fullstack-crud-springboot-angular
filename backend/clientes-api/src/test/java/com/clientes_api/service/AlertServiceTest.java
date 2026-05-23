package com.clientes_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TelegramService telegramService;

    @InjectMocks
    private AlertService alertService;

    @Test
    void handleDatadogPayload_enviaTelegramComAlerta() throws Exception {
        JsonNode root = objectMapper.readTree(
                """
                {
                  "title": "High error rate",
                  "text": "More than 5 errors in 1 minute",
                  "alert_type": "error",
                  "date": 1710000000,
                  "service": "erp-api",
                  "endpoint": "/pagamentos"
                }
                """
        );

        alertService.handleDatadogPayload(root);

        verify(telegramService).sendMessage(argThat(msg ->
                msg.contains("ALERTA DATADOG")
                        && msg.contains("Status: ALERT")
                        && msg.contains("erp-api")
                        && msg.contains("More than 5 errors")
                        && msg.contains("Título: High error rate")
                        && msg.contains("/pagamentos")
                        && msg.contains("Hora:")
        ));
    }

    @Test
    void normalize_recuperado_usaEmojiVerde() throws Exception {
        JsonNode root = objectMapper.readTree(
                """
                {
                  "title": "Monitor recovered",
                  "text": "All checks passed",
                  "alert_type": "success",
                  "date": 1710000000
                }
                """
        );

        String formatted = alertService.formatTelegramMessage(alertService.normalize(root));
        assertThat(formatted).contains("RECUPERADO").doesNotContain("Status: ALERT");
    }
}

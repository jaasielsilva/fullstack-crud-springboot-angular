package com.clientes_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import com.clientes_api.service.AlertService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testa o controller sem subir o contexto Spring (evita beans de segurança/JPA no slice {@code WebMvcTest} do Boot 4).
 */
@ExtendWith(MockitoExtension.class)
class DatadogWebhookControllerTest {

    private static final String SECRET = "test-webhook-secret-xyz";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AlertService alertService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DatadogWebhookController controller = new DatadogWebhookController(
                objectMapper,
                alertService,
                SECRET,
                "",
                false
        );
        mockMvc = standaloneSetup(controller).build();
    }

    @Test
    void semToken_retorna401() throws Exception {
        mockMvc.perform(
                        post("/api/webhooks/datadog")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"x\"}")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"unauthorized\"}"));

        verify(alertService, never()).handleDatadogPayload(any());
    }

    @Test
    void tokenInvalido_retorna401() throws Exception {
        mockMvc.perform(
                        post("/api/webhooks/datadog")
                                .header(DatadogWebhookController.HEADER_DATADOG_TOKEN, "wrong")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"x\"}")
                )
                .andExpect(status().isUnauthorized());

        verify(alertService, never()).handleDatadogPayload(any());
    }

    @Test
    void tokenValido_processaEretorna200() throws Exception {
        mockMvc.perform(
                        post("/api/webhooks/datadog")
                                .header(DatadogWebhookController.HEADER_DATADOG_TOKEN, SECRET)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"CPU high\",\"text\":\"90%\",\"alert_type\":\"error\",\"date\":1710000000}")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"processed\"}"));

        verify(alertService).handleDatadogPayload(any());
    }
}

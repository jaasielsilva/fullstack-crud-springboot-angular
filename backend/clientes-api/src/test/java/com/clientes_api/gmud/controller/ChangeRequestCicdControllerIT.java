package com.clientes_api.gmud.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ChangeRequestCicdControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void start_semToken_retorna401() throws Exception {
        mockMvc.perform(post("/api/internal/gmud/deploy-events/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"environment":"HML","title":"t","pipelineRunId":"x1"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void start_comToken_valido() throws Exception {
        mockMvc.perform(post("/api/internal/gmud/deploy-events/start")
                        .header("X-Deploy-Token", "test-gmud-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environment": "HML",
                                  "title": "Deploy test",
                                  "pipelineRunId": "pipeline-it-001",
                                  "commitSha": "abc1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changeId").exists())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void status_porPipelineRunId() throws Exception {
        mockMvc.perform(post("/api/internal/gmud/deploy-events/start")
                        .header("X-Deploy-Token", "test-gmud-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environment": "PROD",
                                  "title": "Deploy status test",
                                  "pipelineRunId": "pipeline-it-status-001"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/internal/gmud/deploy-events/status/pipeline-it-status-001")
                        .header("X-Deploy-Token", "test-gmud-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_APPROVAL"));
    }
}

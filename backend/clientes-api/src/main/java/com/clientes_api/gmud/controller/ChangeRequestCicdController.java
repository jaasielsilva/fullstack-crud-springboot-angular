package com.clientes_api.gmud.controller;

import com.clientes_api.gmud.dto.CicdDeployEventDTO;
import com.clientes_api.gmud.dto.CicdDeployResponseDTO;
import com.clientes_api.gmud.service.ChangeRequestCicdService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "GMUD CI/CD", description = "Eventos de deploy para integração com pipeline")
@RestController
@RequestMapping("/api/internal/gmud/deploy-events")
public class ChangeRequestCicdController {

    private final ChangeRequestCicdService cicdService;

    public ChangeRequestCicdController(ChangeRequestCicdService cicdService) {
        this.cicdService = cicdService;
    }

    @GetMapping("/status/{pipelineRunId}")
    public ResponseEntity<CicdDeployResponseDTO> status(@PathVariable String pipelineRunId) {
        return ResponseEntity.ok(cicdService.getStatusByPipelineRunId(pipelineRunId));
    }

    @PostMapping("/start")
    public ResponseEntity<CicdDeployResponseDTO> start(@Valid @RequestBody CicdDeployEventDTO event) {
        return ResponseEntity.ok(cicdService.onDeployStart(event));
    }

    @PostMapping("/success")
    public ResponseEntity<CicdDeployResponseDTO> success(@Valid @RequestBody CicdDeployEventDTO event) {
        return ResponseEntity.ok(cicdService.onDeploySuccess(event));
    }

    @PostMapping("/failure")
    public ResponseEntity<CicdDeployResponseDTO> failure(@Valid @RequestBody CicdDeployEventDTO event) {
        return ResponseEntity.ok(cicdService.onDeployFailure(event));
    }
}

package com.clientes_api.controller;

import com.clientes_api.dto.LoginResponseDTO;
import com.clientes_api.dto.PublicTrialRegistrationDTO;
import com.clientes_api.model.Usuario;
import com.clientes_api.security.TokenService;
import com.clientes_api.service.SubscriptionSnapshotService;
import com.clientes_api.service.TrialSignupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Cadastro trial", description = "Onboarding sem cartão — 7 dias")
public class PublicTrialRegistrationController {

    private final TrialSignupService trialSignupService;
    private final TokenService tokenService;
    private final SubscriptionSnapshotService subscriptionSnapshotService;

    public PublicTrialRegistrationController(TrialSignupService trialSignupService,
                                             TokenService tokenService,
                                             SubscriptionSnapshotService subscriptionSnapshotService) {
        this.trialSignupService = trialSignupService;
        this.tokenService = tokenService;
        this.subscriptionSnapshotService = subscriptionSnapshotService;
    }

    @PostMapping("/cadastro-trial")
    @Operation(summary = "Cria empresa em TRIAL, usuário ADMIN e assinatura TRIAL")
    public ResponseEntity<LoginResponseDTO> cadastroTrial(@Valid @RequestBody PublicTrialRegistrationDTO body) {
        Usuario admin = trialSignupService.cadastrarTrial(body);
        String token = tokenService.gerarToken(admin);
        var snapshot = subscriptionSnapshotService.montar(admin.getTenantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new LoginResponseDTO(token, snapshot));
    }
}

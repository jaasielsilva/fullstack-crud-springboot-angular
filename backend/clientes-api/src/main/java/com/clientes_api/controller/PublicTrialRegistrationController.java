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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(PublicTrialRegistrationController.class);

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
        log.info(
                "PUBLIC | Requisição cadastro-trial recebida | empresaNome={} | email={}",
                body.nomeEmpresa().trim(),
                body.email().trim().toLowerCase()
        );
        Usuario admin = trialSignupService.cadastrarTrial(body);
        String token = tokenService.gerarToken(admin);
        var snapshot = subscriptionSnapshotService.montar(admin.getTenantId());
        log.info(
                "PUBLIC | cadastro-trial finalizado com JWT e snapshot | tenantId={} | adminUserId={}",
                admin.getTenantId(),
                admin.getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(new LoginResponseDTO(token, snapshot));
    }
}

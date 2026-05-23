package com.clientes_api.controller;

import com.clientes_api.model.EmailConfig;
import com.clientes_api.model.Usuario;
import com.clientes_api.repository.EmailConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Configurações", description = "Endpoints de configuração do sistema")
@RestController
@RequestMapping("/api/config/email")
public class EmailConfigController {

    @Autowired
    private EmailConfigRepository repository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmailConfig> getConfig(@AuthenticationPrincipal Usuario usuario) {
        Long tenantId = obterTenantId(usuario);
        return repository.findByTenantId(tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmailConfig> saveConfig(@AuthenticationPrincipal Usuario usuario,
                                                  @RequestBody EmailConfig data) {
        Long tenantId = obterTenantId(usuario);
        EmailConfig config = repository.findByTenantId(tenantId)
                .map(existing -> {
                    existing.setTenantId(tenantId);
                    existing.setHost(data.getHost());
                    existing.setPort(data.getPort());
                    existing.setUsuario(data.getUsuario());
                    existing.setSenha(data.getSenha());
                    existing.setProtocol(data.getProtocol());
                    existing.setAuth(data.getAuth());
                    existing.setStarttls(data.getStarttls());
                    return repository.save(existing);
                })
                .orElseGet(() -> {
                    data.setId(null);
                    data.setTenantId(tenantId);
                    return repository.save(data);
                });
        
        return ResponseEntity.ok(config);
    }

    private Long obterTenantId(Usuario usuario) {
        if (usuario == null || usuario.getTenantId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant do usuário autenticado não encontrado.");
        }
        return usuario.getTenantId();
    }
}

package com.clientes_api.controller;

import com.clientes_api.model.EmailConfig;
import com.clientes_api.repository.EmailConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Configurações", description = "Endpoints de configuração do sistema")
@RestController
@RequestMapping("/api/config/email")
public class EmailConfigController {

    @Autowired
    private EmailConfigRepository repository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmailConfig> getConfig() {
        return repository.findFirstByOrderByIdAsc()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmailConfig> saveConfig(@RequestBody EmailConfig data) {
        EmailConfig config = repository.findFirstByOrderByIdAsc()
                .map(existing -> {
                    existing.setHost(data.getHost());
                    existing.setPort(data.getPort());
                    existing.setUsuario(data.getUsuario());
                    existing.setSenha(data.getSenha());
                    existing.setProtocol(data.getProtocol());
                    existing.setAuth(data.getAuth());
                    existing.setStarttls(data.getStarttls());
                    return repository.save(existing);
                })
                .orElseGet(() -> repository.save(data));
        
        return ResponseEntity.ok(config);
    }
}

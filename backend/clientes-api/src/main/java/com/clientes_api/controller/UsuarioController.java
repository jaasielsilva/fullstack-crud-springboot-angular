package com.clientes_api.controller;

import com.clientes_api.model.Usuario;
import com.clientes_api.model.UsuarioRole;
import com.clientes_api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuários", description = "Endpoints de gerenciamento de usuários administrativos")
public class UsuarioController {

    @Autowired
    private UsuarioRepository repository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<List<Usuario>> listar() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> criar(@RequestBody Usuario data) {
        if (repository.findByLogin(data.getLogin()) != null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "E-mail já cadastrado."));
        }
        
        String encryptedPassword = new BCryptPasswordEncoder().encode(data.getSenha());
        data.setSenha(encryptedPassword);
        
        Usuario salvo = repository.save(data);
        return ResponseEntity.ok(salvo);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Usuario data) {
        return repository.findById(id).map(usuario -> {
            usuario.setLogin(data.getLogin());
            usuario.setUsername(data.getUsername());
            usuario.setRole(data.getRole());
            
            if (data.getSenha() != null && !data.getSenha().isEmpty()) {
                String encryptedPassword = new BCryptPasswordEncoder().encode(data.getSenha());
                usuario.setSenha(encryptedPassword);
            }
            
            Usuario salvo = repository.save(usuario);
            return ResponseEntity.ok(salvo);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Altera apenas o nível de acesso (role) de um usuário")
    public ResponseEntity<?> alterarRole(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String novoRole = payload.get("role");
        if (novoRole == null || novoRole.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "A role é obrigatória."));
        }

        UsuarioRole roleEnum;
        try {
            roleEnum = UsuarioRole.valueOf(novoRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Role inválida. Use ADMIN, GERENTE, VENDEDOR, SUPORTE ou USER."));
        }

        // Impede que o ADMIN logado rebaixe a si mesmo (poderia ficar sem acesso e travar o sistema)
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentLogin = auth.getName();

        return repository.findById(id).map(usuario -> {
            if (usuario.getLogin().equals(currentLogin) && roleEnum != UsuarioRole.ADMIN) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Você não pode rebaixar seu próprio nível de acesso."));
            }
            usuario.setRole(roleEnum);
            Usuario salvo = repository.save(usuario);
            return ResponseEntity.ok(Map.of(
                "id", salvo.getId(),
                "login", salvo.getLogin(),
                "username", salvo.getUsername(),
                "role", salvo.getRole().name(),
                "mensagem", "Permissão atualizada com sucesso."
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        // Busca o usuário logado para impedir a auto-exclusão
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentLogin = auth.getName();

        return repository.findById(id).map(usuario -> {
            if (usuario.getLogin().equals(currentLogin)) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Você não pode excluir seu próprio usuário."));
            }
            repository.delete(usuario);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}

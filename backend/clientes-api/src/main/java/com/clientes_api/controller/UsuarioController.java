package com.clientes_api.controller;

import com.clientes_api.model.Usuario;
import com.clientes_api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Usuários", description = "Endpoints de gestão de usuários do sistema")
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository repository;

    @GetMapping
    public ResponseEntity<List<Usuario>> listar() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping
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
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Usuario data) {
        return repository.findById(id).map(usuario -> {
            usuario.setLogin(data.getLogin());
            usuario.setUsername(data.getUsername());
            usuario.setRole(data.getRole());
            
            // Só atualiza a senha se ela for enviada
            if (data.getSenha() != null && !data.getSenha().isBlank()) {
                String encryptedPassword = new BCryptPasswordEncoder().encode(data.getSenha());
                usuario.setSenha(encryptedPassword);
            }
            
            repository.save(usuario);
            return ResponseEntity.ok(usuario);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        return repository.findById(id).map(usuario -> {
            repository.delete(usuario);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}

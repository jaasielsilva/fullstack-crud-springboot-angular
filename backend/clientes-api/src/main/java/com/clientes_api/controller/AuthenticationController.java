package com.clientes_api.controller;

import com.clientes_api.dto.*;
import com.clientes_api.service.EmailService;
import com.clientes_api.security.TokenService;
import com.clientes_api.model.Usuario;
import com.clientes_api.repository.UsuarioRepository;
import com.clientes_api.service.SubscriptionSnapshotService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.Random;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Autenticação", description = "Endpoints de login e recuperação de senha")
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SubscriptionSnapshotService subscriptionSnapshotService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid AuthenticationDTO data) {
        // ... (login implementation remains same)
        try {
            var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.senha());
            var auth = this.authenticationManager.authenticate(usernamePassword);
            Usuario principal = (Usuario) auth.getPrincipal();
            var token = tokenService.gerarToken(principal);
            var snapshot = subscriptionSnapshotService.montar(principal.getTenantId());
            return ResponseEntity.ok(new LoginResponseDTO(token, snapshot));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Email ou senha incorretos."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("erro", "Erro ao realizar login."));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> perfilAssinatura(@AuthenticationPrincipal Usuario usuario) {
        if (usuario == null || usuario.getTenantId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(subscriptionSnapshotService.montar(usuario.getTenantId()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody @Valid ForgotPasswordDTO data) {
        Usuario usuario = (Usuario) repository.findByLogin(data.email());
        if (usuario == null) {
            // Por segurança, retornamos OK mesmo que o email não exista para evitar enumeração de usuários
            return ResponseEntity.ok(Map.of("mensagem", "Se o e-mail existir em nossa base, um código de recuperação será enviado."));
        }

        // Gera um token numérico de 6 dígitos
        String token = String.format("%06d", new Random().nextInt(999999));
        usuario.setResetToken(token);
        usuario.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15)); // Expira em 15 min
        repository.save(usuario);

        try {
            emailService.enviarEmailRecuperacao(usuario.getLogin(), token);
            return ResponseEntity.ok(Map.of("mensagem", "E-mail de recuperação enviado com sucesso!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("erro", "Erro ao enviar e-mail: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid ResetPasswordDTO data) {
        // Busca usuário pelo token e verifica se não expirou
        List<Usuario> usuarios = repository.findAll(); // Simples para este exemplo, ideal seria um método no repo
        Usuario usuario = usuarios.stream()
                .filter(u -> data.token().equals(u.getResetToken()))
                .filter(u -> u.getResetTokenExpiry() != null && u.getResetTokenExpiry().isAfter(LocalDateTime.now()))
                .findFirst()
                .orElse(null);

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", "Token inválido ou expirado."));
        }

        // Atualiza a senha
        String encryptedPassword = new BCryptPasswordEncoder().encode(data.novaSenha());
        usuario.setSenha(encryptedPassword);
        usuario.setResetToken(null);
        usuario.setResetTokenExpiry(null);
        repository.save(usuario);

        return ResponseEntity.ok(Map.of("mensagem", "Senha redefinida com sucesso!"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterDTO data) {
        try {
            if (this.repository.findByLogin(data.login()) != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        Map.of("erro", "Já existe um usuário cadastrado com este e-mail.")
                );
            }

            // Criptografa a senha antes de salvar no banco de dados (Muito importante)
            String encryptedPassword = new BCryptPasswordEncoder().encode(data.senha());
            Usuario newUser = new Usuario(data.login(), encryptedPassword, data.role());
            newUser.setUsername(data.username());

            this.repository.save(newUser);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of("mensagem", "Usuário criado com sucesso!")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("erro", "Ocorreu um erro ao cadastrar o usuário.")
            );
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Reemite o token com a role atual do banco (útil após o admin liberar permissão)")
    public ResponseEntity<?> refresh() {
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Sessão inválida."));
            }

            String login = auth.getName();
            Usuario usuarioAtualizado = repository.findByLoginOrUsernameGlobal(login);
            if (usuarioAtualizado == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Usuário não encontrado."));
            }

            String token = tokenService.gerarToken(usuarioAtualizado);
            var snapshot = subscriptionSnapshotService.montar(usuarioAtualizado.getTenantId());
            return ResponseEntity.ok(new LoginResponseDTO(token, snapshot));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("erro", "Erro ao renovar token."));
        }
    }

    @PostMapping("/reset-password-forced")
    public ResponseEntity<?> resetPasswordForced(@RequestBody Map<String, String> data) {
        String novaSenha = data.get("novaSenha");
        if (novaSenha == null || novaSenha.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("erro", "A senha deve ter pelo menos 6 caracteres."));
        }

        // Obtém o usuário logado do contexto de segurança
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = (Usuario) auth.getPrincipal();

        // Atualiza a senha e desativa a flag de redefinição forçada
        String encryptedPassword = new BCryptPasswordEncoder().encode(novaSenha);
        usuario.setSenha(encryptedPassword);
        usuario.setRedefinirSenha(false);
        repository.save(usuario);

        return ResponseEntity.ok(Map.of("mensagem", "Senha atualizada com sucesso!"));
    }
}

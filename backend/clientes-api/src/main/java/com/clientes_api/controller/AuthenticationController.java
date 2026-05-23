package com.clientes_api.controller;

import com.clientes_api.dto.*;
import com.clientes_api.config.SecurityLoggingSupport;
import com.clientes_api.service.EmailService;
import com.clientes_api.security.TokenService;
import com.clientes_api.model.Usuario;
import com.clientes_api.repository.UsuarioRepository;
import com.clientes_api.service.SubscriptionSnapshotService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Tag(name = "Autenticação", description = "Endpoints de login e recuperação de senha")
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

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

    @Autowired
    private SecurityLoggingSupport securityLoggingSupport;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid AuthenticationDTO data) {
        String loginRecebido = data.login() == null ? null : data.login().trim();
        log.info("AUTH | Início do fluxo de login | loginRecebido={}", loginRecebido);
        try {
            var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.senha());
            log.info("AUTH | Credenciais repassadas ao AuthenticationManager | login={}", loginRecebido);
            var auth = this.authenticationManager.authenticate(usernamePassword);
            Usuario principal = (Usuario) auth.getPrincipal();
            log.info(
                    "AUTH | Resultado da autenticação | sucesso=true | userId={} | tenantId={} | login={} | role={}",
                    principal.getId(),
                    principal.getTenantId(),
                    principal.getLogin(),
                    principal.getRole()
            );
            var token = tokenService.gerarToken(principal);
            var snapshot = subscriptionSnapshotService.montar(principal.getTenantId());
            log.info(
                    "AUTH | Login concluído | userId={} | tenantId={} | subscriptionStatus={}",
                    principal.getId(),
                    principal.getTenantId(),
                    snapshot.assinaturaStatus()
            );
            return ResponseEntity.ok(new LoginResponseDTO(token, snapshot));
        } catch (BadCredentialsException e) {
            log.warn("AUTH | Resultado da autenticação | sucesso=false | motivo=credenciais_invalidas | login={}", loginRecebido);
            if (securityLoggingSupport.isVerboseAuthErrorLogging()) {
                log.warn("AUTH | Detalhe (ambiente não-produção)", e);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Email ou senha incorretos."));
        } catch (Exception e) {
            log.error("AUTH | Resultado da autenticação | sucesso=false | motivo=erro_interno | login={}", loginRecebido);
            if (securityLoggingSupport.isVerboseAuthErrorLogging()) {
                log.error("AUTH | Stack trace (ambiente não-produção)", e);
            }
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
            emailService.enviarEmailRecuperacao(usuario.getLogin(), token, usuario.getTenantId());
            return ResponseEntity.ok(Map.of("mensagem", "E-mail de recuperação enviado com sucesso!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("erro", "Erro ao enviar e-mail: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid ResetPasswordDTO data) {
        // Busca usuário pelo token e verifica se não expirou
        Usuario usuario = repository.findByResetTokenValido(data.token(), LocalDateTime.now())
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
        log.info("AUTH | Início registro de usuário (endpoint legado) | login={}", data.login());
        try {
            if (this.repository.findByLogin(data.login()) != null) {
                log.warn("AUTH | Registro recusado | motivo=email_duplicado | login={}", data.login());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        Map.of("erro", "Já existe um usuário cadastrado com este e-mail.")
                );
            }

            // Criptografa a senha antes de salvar no banco de dados (Muito importante)
            String encryptedPassword = new BCryptPasswordEncoder().encode(data.senha());
            Usuario newUser = new Usuario(data.login(), encryptedPassword, data.role());
            newUser.setUsername(data.username());

            this.repository.save(newUser);
            log.info(
                    "AUTH | Usuário persistido (register) | userId={} | tenantId={} | login={}",
                    newUser.getId(),
                    newUser.getTenantId(),
                    newUser.getLogin()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of("mensagem", "Usuário criado com sucesso!")
            );
        } catch (Exception e) {
            log.error("AUTH | Erro ao cadastrar usuário | login={}", data.login());
            if (securityLoggingSupport.isVerboseAuthErrorLogging()) {
                log.error("AUTH | Stack trace (ambiente não-produção)", e);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("erro", "Ocorreu um erro ao cadastrar o usuário.")
            );
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Reemite o token com a role atual do banco (útil após o admin liberar permissão)")
    public ResponseEntity<?> refresh() {
        log.info("AUTH | Início refresh de JWT");
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                log.warn("AUTH | Refresh negado | motivo=sessao_invalida");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Sessão inválida."));
            }

            String login = auth.getName();
            // Busca tenant-aware para evitar conflito com @TenantId quando TenantContext estiver setado.
            Usuario usuarioAtualizado = repository.findByLoginOrUsername(login, login);
            if (usuarioAtualizado == null) {
                log.warn("AUTH | Refresh negado | motivo=usuario_nao_encontrado | login={}", login);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Usuário não encontrado."));
            }

            log.info(
                    "AUTH | Refresh | userId={} | tenantId={} | login={}",
                    usuarioAtualizado.getId(),
                    usuarioAtualizado.getTenantId(),
                    login
            );
            String token = tokenService.gerarToken(usuarioAtualizado);
            var snapshot = subscriptionSnapshotService.montar(usuarioAtualizado.getTenantId());
            log.info("AUTH | Refresh concluído | tenantId={}", usuarioAtualizado.getTenantId());
            return ResponseEntity.ok(new LoginResponseDTO(token, snapshot));
        } catch (Exception e) {
            log.error("AUTH | Erro no refresh de token");
            if (securityLoggingSupport.isVerboseAuthErrorLogging()) {
                log.error("AUTH | Stack trace (ambiente não-produção)", e);
            }
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

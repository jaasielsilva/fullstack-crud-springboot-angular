package com.clientes_api.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.clientes_api.config.SecurityLoggingSupport;
import com.clientes_api.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    @Value("${api.security.token.secret:my-secret-key-lexcrm}")
    private String secret;

    @Autowired
    private SecurityLoggingSupport securityLoggingSupport;

    public String gerarToken(Usuario usuario) {
        log.info(
                "JWT | Iniciando geração | subject={} | tenantId={} | role={} | mustChangePassword={}",
                usuario.getLogin(),
                usuario.getTenantId(),
                usuario.getRole(),
                usuario.getRedefinirSenha()
        );
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            Instant expiraEm = genExpirationDate();
            String jwt = JWT.create()
                    .withIssuer("lexcrm-api")
                    .withSubject(usuario.getLogin())
                    .withClaim("role", usuario.getRole().name())
                    .withClaim("tenantId", usuario.getTenantId())
                    .withClaim("mustChangePassword", usuario.getRedefinirSenha()) // Nova Claim
                    .withExpiresAt(expiraEm)
                    .sign(algorithm);
            log.info(
                    "JWT | Token gerado com sucesso | subject={} | tenantId={} | expiraEm={}",
                    usuario.getLogin(),
                    usuario.getTenantId(),
                    expiraEm
            );
            return jwt;
        } catch (JWTCreationException exception) {
            log.error("JWT | Falha na criação do token | subject={} | tenantId={}", usuario.getLogin(), usuario.getTenantId());
            if (securityLoggingSupport.isVerboseAuthErrorLogging()) {
                log.error("JWT | Detalhe (ambiente não-produção)", exception);
            }
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    public String validarToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("lexcrm-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            log.warn("JWT | Validação falhou | motivo=assinatura_issuer_expiracao_ou_claim");
            if (securityLoggingSupport.isVerboseAuthErrorLogging()) {
                log.warn("JWT | Detalhe (ambiente não-produção)", exception);
            }
            return "";
        }
    }

    public Long getTenantIdFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("lexcrm-api")
                    .build()
                    .verify(token)
                    .getClaim("tenantId").asLong();
        } catch (JWTVerificationException exception) {
            log.warn("JWT | Não foi possível extrair tenantId do token");
            if (securityLoggingSupport.isVerboseAuthErrorLogging()) {
                log.warn("JWT | Detalhe (ambiente não-produção)", exception);
            }
            return null;
        }
    }

    private Instant genExpirationDate() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}

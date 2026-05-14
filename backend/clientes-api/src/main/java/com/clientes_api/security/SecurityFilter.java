package com.clientes_api.security;

import com.clientes_api.config.SecurityLoggingSupport;
import com.clientes_api.config.TenantContext;
import com.clientes_api.config.TenantSecurityLogger;
import com.clientes_api.model.Usuario;
import com.clientes_api.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SecurityLoggingSupport securityLoggingSupport;

    @Autowired
    private TenantSecurityLogger tenantSecurityLogger;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = this.recoverToken(request);
        boolean bearerPresent = token != null;
        if (bearerPresent) {
            logger.info("AUTH-JWT | Início validação Bearer | método={} | path={}", request.getMethod(), request.getRequestURI());
        }

        try {
            if (token != null) {
                Long tenantAntesJwt = TenantContext.getCurrentTenant();
                logger.info("AUTH-JWT | TenantContext atual (antes do JWT) | tenantId={}", tenantAntesJwt);

                String login = tokenService.validarToken(token);
                if (login == null || login.isEmpty()) {
                    logger.warn("AUTH-JWT | Token inválido ou expirado | path={}", request.getRequestURI());
                } else {
                    logger.info("AUTH-JWT | Login extraído do JWT | login={}", login);
                    Long tenantFromToken = tokenService.getTenantIdFromToken(token);
                    logger.info("AUTH-JWT | TenantId extraído do JWT | tenantId={}", tenantFromToken);

                    if (tenantFromToken != null) {
                        // Define o tenant antes de materializar Usuario (@TenantId), evitando conflito com TenantContext vazio (tenant=0).
                        TenantContext.setCurrentTenant(tenantFromToken);
                        logger.info("AUTH-JWT | TenantContext após set (claim JWT) | tenantId={}", TenantContext.getCurrentTenant());
                    } else {
                        logger.warn("AUTH-JWT | JWT sem claim tenantId — Hibernate pode usar tenant 0 até materializar usuário | login={}", login);
                        tenantSecurityLogger.logMissingTenantContext("jwt_sem_claim_tenantId|login=" + login);
                    }

                    Usuario usuario = usuarioRepository.findByLoginOrUsername(login, login);
                    if (usuario == null) {
                        logger.warn("AUTH-JWT | Usuário não encontrado no banco | login={} | tenantContext={}", login, TenantContext.getCurrentTenant());
                    } else {
                        Long tenantIdUsuario = usuario.getTenantId();
                        logger.info(
                                "AUTH-JWT | Usuário encontrado | userId={} | tenantIdUsuario={} | login={}",
                                usuario.getId(),
                                tenantIdUsuario,
                                login
                        );
                        if (tenantFromToken != null && tenantIdUsuario != null
                                && !tenantFromToken.equals(tenantIdUsuario)) {
                            logger.warn(
                                    "AUTH-JWT | Resultado autenticação | sucesso=false | motivo=tenant_jwt_difere_usuario | tenantJwt={} | tenantUsuario={} | login={}",
                                    tenantFromToken,
                                    tenantIdUsuario,
                                    login
                            );
                            tenantSecurityLogger.logCrossTenantAccessDenied(tenantFromToken, tenantIdUsuario, "jwt_vs_usuario");
                        } else {
                            UserDetails user = usuario;
                            var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            logger.info(
                                    "AUTH-JWT | Resultado autenticação | sucesso=true | userId={} | tenantId={} | authorities={}",
                                    usuario.getId(),
                                    tenantIdUsuario,
                                    user.getAuthorities()
                            );
                        }
                    }
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("AUTH-JWT | Erro no filtro de segurança | path={} | mensagem={}", request.getRequestURI(), e.getMessage());
            if (securityLoggingSupport.isVerboseAuthErrorLogging()) {
                logger.error("AUTH-JWT | Stack trace (ambiente não-produção)", e);
            }
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } finally {
            if (bearerPresent) {
                logger.info("AUTH-JWT | TenantContext limpo ao final do filtro (fim da requisição)");
            }
            TenantContext.clear();
        }
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}

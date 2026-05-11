package com.clientes_api.security;

import com.clientes_api.config.TenantContext;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = this.recoverToken(request);
        
        try {
            if (token != null) {
                String login = tokenService.validarToken(token);
                if (login != null && !login.isEmpty()) {
                    Long tenantFromToken = tokenService.getTenantIdFromToken(token);
                    if (tenantFromToken != null) {
                        // Define o tenant antes de materializar Usuario (@TenantId), evitando conflito com TenantContext vazio (tenant=0).
                        TenantContext.setCurrentTenant(tenantFromToken);
                    }

                    Usuario usuario = usuarioRepository.findByLoginOrUsername(login, login);
                    if (usuario != null) {
                        Long tenantIdUsuario = usuario.getTenantId();
                        if (tenantFromToken != null && tenantIdUsuario != null
                                && !tenantFromToken.equals(tenantIdUsuario)) {
                            logger.warn("Token rejeitado: tenantId do JWT não confere com o usuário (login={})", login);
                        } else {
                            // TenantContext já foi definido com o claim; aqui mantemos apenas logging/auth.
                            logger.debug("Auth JWT: login={}, tenant={}", login, tenantIdUsuario);
                            UserDetails user = usuario;
                            var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    }
                }
            }
            
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Erro no filtro de segurança: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } finally {
            TenantContext.clear();
        }
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}

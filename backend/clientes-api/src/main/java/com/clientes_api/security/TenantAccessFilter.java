package com.clientes_api.security;

import com.clientes_api.model.Tenant;
import com.clientes_api.model.Usuario;
import com.clientes_api.model.enums.StatusEmpresa;
import com.clientes_api.repository.TenantRepository;
import com.clientes_api.service.EmpresaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Bloqueia rotas da API para empresas {@link StatusEmpresa#BLOQUEADA} ou {@link StatusEmpresa#CANCELADA},
 * exceto autenticação, checkout e consulta de contexto de assinatura.
 */
@Component
public class TenantAccessFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepository;
    private final EmpresaService empresaService;
    private final ObjectMapper objectMapper;

    public TenantAccessFilter(TenantRepository tenantRepository,
                              EmpresaService empresaService,
                              ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.empresaService = empresaService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Usuario usuario)) {
            filterChain.doFilter(request, response);
            return;
        }

        Long tenantId = usuario.getTenantId();
        if (tenantId == null || empresaService.isMatriz(tenantId)) {
            filterChain.doFilter(request, response);
            return;
        }

        Tenant empresa = tenantRepository.findById(tenantId).orElse(null);
        if (empresa == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (empresa.getStatus() != StatusEmpresa.BLOQUEADA && empresa.getStatus() != StatusEmpresa.CANCELADA) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isAllowedPathWhenCommerciallyBlocked(path, request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "codigo", "EMPRESA_BLOQUEADA",
                "mensagem", "Sua empresa está bloqueada. Contrate um plano para continuar utilizando o ERP."
        )));
    }

    private boolean isAllowedPathWhenCommerciallyBlocked(String path, String method) {
        if (path.startsWith("/api/auth/me")
                || path.startsWith("/api/auth/reset-password-forced")
                || path.startsWith("/api/public/checkout")
                || path.startsWith("/api/public/planos")) {
            return true;
        }
        return "POST".equalsIgnoreCase(method) && path.matches("/api/pedidos/\\d+/checkout");
    }
}

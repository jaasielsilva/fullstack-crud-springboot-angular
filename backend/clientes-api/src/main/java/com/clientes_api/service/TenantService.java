package com.clientes_api.service;

import com.clientes_api.dto.TenantRegistrationDTO;
import com.clientes_api.model.Tenant;
import com.clientes_api.model.UsuarioRole;
import com.clientes_api.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TenantService {

    private static final Logger logger = LoggerFactory.getLogger(TenantService.class);

    @Autowired
    private TenantRepository tenantRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public List<Tenant> listarTodos() {
        return tenantRepository.findAll();
    }

    @Transactional
    public Tenant cadastrarEmpresa(TenantRegistrationDTO data) {
        logger.info("Iniciando onboarding para a empresa: {}", data.nomeEmpresa());
        
        try {
            // 1. Cria a Empresa
            Tenant novoTenant = new Tenant();
            novoTenant.setNome(data.nomeEmpresa());
            novoTenant.setCnpj(data.cnpj());
            novoTenant.setAtivo(true);
            tenantRepository.save(novoTenant);
            logger.info("Tenant criado com ID: {}", novoTenant.getId());

            // 2. Prepara os dados de Auditoria (Manual para a Native Query)
            String creator = SecurityContextHolder.getContext().getAuthentication().getName();
            LocalDateTime now = LocalDateTime.now();
            String senhaCripto = new BCryptPasswordEncoder().encode(data.senhaAdmin());
            
            // 3. Insere o Administrador com todos os campos necessários (Auditoria + Tenant)
            entityManager.createNativeQuery(
                "INSERT INTO usuarios (login, username, senha, role, tenant_id, redefinir_senha, created_at, updated_at, created_by, updated_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )
            .setParameter(1, data.emailAdmin())
            .setParameter(2, data.usernameAdmin())
            .setParameter(3, senhaCripto)
            .setParameter(4, UsuarioRole.ADMIN.name())
            .setParameter(5, novoTenant.getId())
            .setParameter(6, true)
            .setParameter(7, now)
            .setParameter(8, now)
            .setParameter(9, creator)
            .setParameter(10, creator)
            .executeUpdate();

            logger.info("Administrador inicial {} criado para o Tenant {}", data.emailAdmin(), novoTenant.getId());
            return novoTenant;
        } catch (Exception e) {
            logger.error("Erro no onboarding: {}", e.getMessage());
            throw new RuntimeException("Falha ao cadastrar empresa: " + e.getMessage());
        }
    }
}

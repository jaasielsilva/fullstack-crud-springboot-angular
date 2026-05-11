package com.clientes_api.service;

import com.clientes_api.dto.TenantRegistrationDTO;
import com.clientes_api.model.Tenant;
import com.clientes_api.model.UsuarioRole;
import com.clientes_api.model.enums.StatusEmpresa;
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
        logger.info("TENANT | Início onboarding corporativo | nomeEmpresa={} | cnpj={}", data.nomeEmpresa(), data.cnpj());

        try {
            // 1. Cria a Empresa
            Tenant novoTenant = new Tenant();
            novoTenant.setNome(data.nomeEmpresa());
            novoTenant.setDocumento(data.cnpj());
            novoTenant.setStatus(StatusEmpresa.ATIVA);
            novoTenant.setTrialInicio(null);
            novoTenant.setTrialFim(null);
            novoTenant.setAtivo(true);
            tenantRepository.save(novoTenant);
            logger.info(
                    "TENANT | Empresa persistida (Hibernate JPA) | tenantId={} | nome={} | ativo={}",
                    novoTenant.getId(),
                    novoTenant.getNome(),
                    novoTenant.getAtivo()
            );

            // 2. Prepara os dados de Auditoria (Manual para a Native Query)
            String creator = SecurityContextHolder.getContext().getAuthentication().getName();
            LocalDateTime now = LocalDateTime.now();
            String senhaCripto = new BCryptPasswordEncoder().encode(data.senhaAdmin());
            logger.info(
                    "TENANT | Inserindo ADMIN via native SQL (fora do filtro Hibernate tenant) | tenantId={} | emailAdmin={} | createdBy={}",
                    novoTenant.getId(),
                    data.emailAdmin(),
                    creator
            );

            // 3. Insere o Administrador com todos os campos necessários (Auditoria + Tenant)
            int inserted = entityManager.createNativeQuery(
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

            logger.info(
                    "TENANT | Native INSERT usuarios concluído | linhasAfetadas={} | tenantId={} | emailAdmin={}",
                    inserted,
                    novoTenant.getId(),
                    data.emailAdmin()
            );
            logger.info("TENANT | Onboarding concluído | tenantId={} | adminEmail={}", novoTenant.getId(), data.emailAdmin());
            return novoTenant;
        } catch (Exception e) {
            logger.error("TENANT | Falha no onboarding | nomeEmpresa={} | mensagem={}", data.nomeEmpresa(), e.getMessage());
            throw new RuntimeException("Falha ao cadastrar empresa: " + e.getMessage());
        }
    }
}

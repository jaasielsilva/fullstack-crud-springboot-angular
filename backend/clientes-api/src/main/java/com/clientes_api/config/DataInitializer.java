package com.clientes_api.config;

import com.clientes_api.model.Tenant;
import com.clientes_api.model.Usuario;
import com.clientes_api.model.UsuarioRole;
import com.clientes_api.repository.TenantRepository;
import com.clientes_api.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.data-initializer.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private TenantRepository tenantRepository;

    @Override
    public void run(String... args) throws Exception {
        try {
            logger.info("Iniciando processo de inicialização de dados...");

            // Se não houver tenants, criamos um padrão
            Tenant defaultTenant;
            if (tenantRepository.count() == 0) {
                defaultTenant = tenantRepository.save(new Tenant("LexCRM Matriz"));
                logger.info("✅ Tenant padrão criado: {}", defaultTenant.getNome());
            } else {
                defaultTenant = tenantRepository.findAll().get(0);
                logger.info("ℹ️ Utilizando Tenant existente: {}", defaultTenant.getNome());
            }

            // IMPORTANTE: Definir o contexto de Tenant para permitir a gravação inicial
            TenantContext.setCurrentTenant(defaultTenant.getId());
            logger.info("Contexto de Tenant definido para ID: {}", defaultTenant.getId());

            // Verifica se o banco de dados de usuários está vazio
            if (repository.count() == 0) {
                logger.info("Banco de usuários vazio. Criando administrador padrão...");
                
                String encryptedPassword = new BCryptPasswordEncoder().encode("123");
                
                Usuario admin = new Usuario("admin@lexcrm.com.br", encryptedPassword, UsuarioRole.ADMIN);
                admin.setUsername("admin.lex");
                admin.setTenantId(defaultTenant.getId()); // Vincula ao tenant
                admin.setRedefinirSenha(false); // Admin matriz não precisa resetar
                
                repository.save(admin);
                
                logger.info("✅ Usuário admin@lexcrm.com.br criado com sucesso.");
            }
        } catch (Exception e) {
            logger.error("❌ ERRO CRÍTICO NA INICIALIZAÇÃO: {}", e.getMessage(), e);
            throw e;
        } finally {
            // Limpa o contexto após a inicialização
            TenantContext.clear();
            logger.info("Processo de inicialização finalizado e contexto limpo.");
        }
    }
}
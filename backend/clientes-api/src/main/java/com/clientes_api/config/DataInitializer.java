package com.clientes_api.config;

import com.clientes_api.model.Plano;
import com.clientes_api.model.Tenant;
import com.clientes_api.model.Usuario;
import com.clientes_api.model.UsuarioRole;
import com.clientes_api.model.enums.StatusEmpresa;
import com.clientes_api.model.enums.TipoPlano;
import com.clientes_api.repository.PlanoRepository;
import com.clientes_api.repository.TenantRepository;
import com.clientes_api.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "app.data-initializer.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PlanoRepository planoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@lexcrm.com.br}")
    private String adminEmail;

    @Value("${app.admin.senha:admin@LexCRM2025}")
    private String adminSenha;

    @Override
    public void run(String... args) throws Exception {
        try {
            logger.info("Iniciando processo de inicialização de dados...");

            if (planoRepository.count() == 0) {
                Plano basico = new Plano();
                basico.setNome("Plano Básico");
                basico.setDescricao("Clientes, Produtos, Usuários, Pedidos/Vendas e Dashboard básico.");
                basico.setValor(new BigDecimal("59.90"));
                basico.setTipo(TipoPlano.BASICO);
                basico.setAtivo(true);
                basico.setRecursosLiberados("[\"CLIENTES\",\"PRODUTOS\",\"USUARIOS\",\"PEDIDOS\",\"DASHBOARD_BASICO\"]");

                Plano premium = new Plano();
                premium.setNome("Plano Premium");
                premium.setDescricao("Dashboard completo, Financeiro, Pedidos/Vendas, Relatórios e Suporte prioritário.");
                premium.setValor(new BigDecimal("99.90"));
                premium.setTipo(TipoPlano.PREMIUM);
                premium.setAtivo(true);
                premium.setRecursosLiberados("[\"CLIENTES\",\"PRODUTOS\",\"USUARIOS\",\"DASHBOARD_COMPLETO\",\"FINANCEIRO\",\"PEDIDOS\",\"RELATORIOS\",\"SUPORTE_PRIORITARIO\"]");

                planoRepository.save(basico);
                planoRepository.save(premium);
                logger.info("✅ Planos Básico e Premium criados no catálogo.");
            }

            // Se não houver tenants, criamos um padrão
            Tenant defaultTenant;
            if (!tenantRepository.existsByEmailIgnoreCase(adminEmail)) {
                Tenant matriz = new Tenant("LexCRM Matriz");
                matriz.setStatus(StatusEmpresa.ATIVA);
                matriz.setEmail(adminEmail);
                matriz.setTrialInicio(null);
                matriz.setTrialFim(null);
                defaultTenant = tenantRepository.save(matriz);
                logger.info("✅ Tenant padrão criado: {}", defaultTenant.getNome());
            } else {
                defaultTenant = tenantRepository.findAll().stream()
                        .filter(t -> adminEmail.equalsIgnoreCase(t.getEmail()))
                        .findFirst()
                        .orElse(tenantRepository.findAll().get(0));
                logger.info("ℹ️ Utilizando Tenant existente: {}", defaultTenant.getNome());
            }

            // IMPORTANTE: Definir o contexto de Tenant para permitir a gravação inicial
            TenantContext.setCurrentTenant(defaultTenant.getId());
            logger.info("Contexto de Tenant definido para ID: {}", defaultTenant.getId());

            // Usa query global (sem filtro de tenant) para verificar se o admin já existe
            if (repository.countByLoginGlobal(adminEmail) == 0) {
                logger.info("Admin não encontrado. Criando administrador padrão...");

                String encryptedPassword = passwordEncoder.encode(adminSenha);

                Usuario admin = new Usuario(adminEmail, encryptedPassword, UsuarioRole.ADMIN);
                admin.setUsername("admin.lex");
                admin.setTenantId(defaultTenant.getId());
                admin.setRedefinirSenha(false);

                repository.save(admin);

                logger.info("✅ Usuário {} criado com sucesso.", adminEmail);
            } else {
                logger.info("ℹ️ Admin {} já existe, pulando criação.", adminEmail);
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
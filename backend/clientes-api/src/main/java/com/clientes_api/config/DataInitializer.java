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
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
            if (tenantRepository.count() == 0) {
                Tenant matriz = new Tenant("LexCRM Matriz");
                matriz.setStatus(StatusEmpresa.ATIVA);
                matriz.setEmail("admin@lexcrm.com.br");
                matriz.setTrialInicio(null);
                matriz.setTrialFim(null);
                defaultTenant = tenantRepository.save(matriz);
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
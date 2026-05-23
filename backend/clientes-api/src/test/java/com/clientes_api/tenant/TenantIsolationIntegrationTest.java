package com.clientes_api.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.clientes_api.config.TenantContext;
import com.clientes_api.exception.ResourceNotFoundException;
import com.clientes_api.model.Cliente;
import com.clientes_api.repository.ClienteRepository;
import com.clientes_api.service.ClienteService;

@SpringBootTest
class TenantIsolationIntegrationTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void limparClientes() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            TenantContext.clear();
            clienteRepository.deleteAll();
        });
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void hibernateTenantFilterRestringeConsultasJpql() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            TenantContext.clear();
            Cliente c1 = new Cliente();
            c1.setNome("T1");
            c1.setEmail("t1-isolation@test.local");
            c1.setTenantId(1L);
            c1.setAtivo(true);
            clienteRepository.saveAndFlush(c1);
        });
        tx.executeWithoutResult(status -> {
            TenantContext.clear();
            Cliente c2 = new Cliente();
            c2.setNome("T2");
            c2.setEmail("t2-isolation@test.local");
            c2.setTenantId(2L);
            c2.setAtivo(true);
            clienteRepository.saveAndFlush(c2);
        });

        TenantContext.setCurrentTenant(1L);
        try {
            tx.executeWithoutResult(status -> {
                assertEquals(1, clienteRepository.findAll().size());
                assertEquals(1, clienteRepository.findAllByTenantId(1L).size());
                assertEquals(0, clienteRepository.findAllByTenantId(2L).size());
            });
        } finally {
            TenantContext.clear();
        }

        TenantContext.setCurrentTenant(2L);
        try {
            tx.executeWithoutResult(status -> {
                assertEquals(1, clienteRepository.findAll().size());
            });
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void buscaPorIdValidaTenantExplicitamente() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Long idOutroTenant = tx.execute(status -> {
            TenantContext.clear();
            Cliente c2 = new Cliente();
            c2.setNome("Outro");
            c2.setEmail("outro-isolation@test.local");
            c2.setTenantId(2L);
            c2.setAtivo(true);
            return clienteRepository.save(c2).getId();
        });

        TenantContext.setCurrentTenant(1L);
        try {
            tx.executeWithoutResult(status -> {
                assertThrows(ResourceNotFoundException.class, () -> clienteService.buscarPorId(idOutroTenant));
            });
        } finally {
            TenantContext.clear();
        }
    }
}

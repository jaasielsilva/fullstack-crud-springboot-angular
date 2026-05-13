package com.clientes_api.service;

import com.clientes_api.model.Assinatura;
import com.clientes_api.model.Pagamento;
import com.clientes_api.model.Plano;
import com.clientes_api.model.Tenant;
import com.clientes_api.model.enums.StatusPagamento;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbacatePayWebhookServiceTest {

    @Mock
    private AssinaturaService assinaturaService;

    @Mock
    private EmpresaService empresaService;

    @Mock
    private AssinaturaAtivacaoService assinaturaAtivacaoService;

    @Mock
    private PagamentoService pagamentoService;

    private AbacatePayWebhookService service;

    @BeforeEach
    void setUp() {
        service = new AbacatePayWebhookService(
                new ObjectMapper(),
                assinaturaService,
                empresaService,
                assinaturaAtivacaoService,
                pagamentoService);
        ReflectionTestUtils.setField(service, "configuredWebhookSecret", "");
        ReflectionTestUtils.setField(service, "webhookHmacKey", "");
        ReflectionTestUtils.setField(service, "logWebhookInbound", false);
    }

    @Test
    void checkoutCompletoPago_persistePagamentoEAtivaAssinatura() {
        String body = """
                {
                  "id": "evt_abacate_1",
                  "event": "checkout.completed",
                  "data": {
                    "checkout": {
                      "id": "bill_x",
                      "externalId": "EMPRESA_1_PLANO_1_ASSINATURA_1",
                      "status": "PAID",
                      "amount": "89.90"
                    }
                  }
                }
                """;

        Plano plano = new Plano();
        plano.setId(1L);
        plano.setValor(new BigDecimal("99.00"));

        Assinatura assinatura = new Assinatura();
        assinatura.setId(1L);
        assinatura.setPlano(plano);
        assinatura.setAbacatePayBillingId("bill_x");

        Tenant empresa = new Tenant();
        empresa.setId(1L);

        when(assinaturaService.buscarPorIdETenantOuErro(1L, 1L)).thenReturn(assinatura);
        when(empresaService.buscarPorIdOuErro(1L)).thenReturn(empresa);
        when(pagamentoService.buscarPorMercadoPagoId(any())).thenReturn(Optional.empty());

        assertTrue(service.processar(body, null, null));

        ArgumentCaptor<Pagamento> pagCap = ArgumentCaptor.forClass(Pagamento.class);
        verify(pagamentoService).salvar(pagCap.capture());
        Pagamento p = pagCap.getValue();
        assertEquals(StatusPagamento.APPROVED, p.getStatus());
        assertEquals("ABACATE_PAY", p.getMetodoPagamento());
        assertEquals("EMPRESA_1_PLANO_1_ASSINATURA_1", p.getExternalReference());
        assertEquals(new BigDecimal("89.90"), p.getValor());
        assertTrue(p.getMercadoPagoPaymentId() != null && p.getMercadoPagoPaymentId().length() <= 64);

        verify(assinaturaAtivacaoService).ativarAssinaturaEEmpresa(eq(assinatura), eq(empresa), any(), isNull());
        verify(assinaturaService).salvar(assinatura);
        verify(empresaService).salvar(empresa);
    }

    @Test
    void webhookSecretIncorreto_naoPersistePagamento() {
        ReflectionTestUtils.setField(service, "configuredWebhookSecret", "segredo");
        String body = """
                {"id":"e1","event":"checkout.completed","data":{"checkout":{"id":"b1","externalId":"EMPRESA_1_PLANO_1_ASSINATURA_1","status":"PAID"}}}
                """;

        assertFalse(service.processar(body, "errado", null));

        verifyNoInteractions(pagamentoService);
        verifyNoInteractions(assinaturaAtivacaoService);
    }

    @Test
    void checkoutIdDiferenteDoSalvo_naoAtivaNemPersistePagamento() {
        String body = """
                {"id":"e1","event":"checkout.completed","data":{"checkout":{"id":"bill_outro","externalId":"EMPRESA_1_PLANO_1_ASSINATURA_1","status":"PAID"}}}
                """;

        Plano plano = new Plano();
        plano.setId(1L);
        Assinatura assinatura = new Assinatura();
        assinatura.setPlano(plano);
        assinatura.setAbacatePayBillingId("bill_esperado");

        when(assinaturaService.buscarPorIdETenantOuErro(1L, 1L)).thenReturn(assinatura);

        assertTrue(service.processar(body, null, null));

        verify(pagamentoService, never()).salvar(any());
        verifyNoInteractions(assinaturaAtivacaoService);
    }
}

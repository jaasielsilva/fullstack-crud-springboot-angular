package com.clientes_api.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.clientes_api.config.TenantContext;
import com.clientes_api.dto.ItemPedidoRequestDTO;
import com.clientes_api.dto.PedidoRequestDTO;
import com.clientes_api.model.Cliente;
import com.clientes_api.model.Produto;
import com.clientes_api.model.Tenant;
import com.clientes_api.model.Usuario;
import com.clientes_api.model.UsuarioRole;
import com.clientes_api.model.enums.StatusEmpresa;
import com.clientes_api.repository.ClienteRepository;
import com.clientes_api.repository.PedidoRepository;
import com.clientes_api.repository.ProdutoRepository;
import com.clientes_api.repository.TenantRepository;
import com.clientes_api.repository.UsuarioRepository;
import com.clientes_api.security.TokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integração HTTP real (porta aleatória) com dois tenants: JWT com {@code tenantId}
 * e isolamento em clientes, produtos, pedidos, usuários e dashboard.
 * <p>
 * O seed usa {@code PROPAGATION_REQUIRES_NEW} para commitar antes das requisições HTTP.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class MultiTenantApiIsolationIT {

    private static final HttpClient HTTP = HttpClient.newBuilder().build();

    @LocalServerPort
    private int port;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtTenantA;
    private String jwtTenantB;
    private Long clienteIdA;
    private Long clienteIdB;
    private Long produtoIdA;
    private Long usuarioIdB;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @BeforeEach
    void setUp() {
        TransactionTemplate seedTx = new TransactionTemplate(transactionManager);
        seedTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        seedTx.executeWithoutResult(status -> seedTwoTenants());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        TransactionTemplate cleanTx = new TransactionTemplate(transactionManager);
        cleanTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        cleanTx.executeWithoutResult(status -> {
            pedidoRepository.deleteAll();
            produtoRepository.deleteAll();
            clienteRepository.deleteAll();
            usuarioRepository.deleteAll();
            tenantRepository.deleteAll();
        });
    }

    private void seedTwoTenants() {
        String s = UUID.randomUUID().toString().substring(0, 8);

        Tenant t1 = new Tenant();
        t1.setNome("MT-ISO-A-" + s);
        t1.setDocumento("11111111000191");
        t1.setEmail("emp-a-" + s + "@test.local");
        t1.setStatus(StatusEmpresa.ATIVA);
        t1 = tenantRepository.save(t1);

        Tenant t2 = new Tenant();
        t2.setNome("MT-ISO-B-" + s);
        t2.setDocumento("22222222000192");
        t2.setEmail("emp-b-" + s + "@test.local");
        t2.setStatus(StatusEmpresa.ATIVA);
        t2 = tenantRepository.save(t2);

        String loginA = "mt-a-" + s + "@test.local";
        String loginB = "mt-b-" + s + "@test.local";
        String hash = passwordEncoder.encode("SenhaSegura1!");

        Usuario uA = new Usuario();
        uA.setLogin(loginA);
        uA.setUsername(loginA);
        uA.setSenha(hash);
        uA.setRole(UsuarioRole.ADMIN);
        uA.setTenantId(t1.getId());
        uA.setRedefinirSenha(false);
        uA = usuarioRepository.save(uA);

        Usuario uB = new Usuario();
        uB.setLogin(loginB);
        uB.setUsername(loginB);
        uB.setSenha(hash);
        uB.setRole(UsuarioRole.ADMIN);
        uB.setTenantId(t2.getId());
        uB.setRedefinirSenha(false);
        uB = usuarioRepository.save(uB);
        usuarioIdB = uB.getId();

        Cliente cA = new Cliente();
        cA.setNome("Cliente A");
        cA.setEmail("cli-a-" + s + "@test.local");
        cA.setTenantId(t1.getId());
        cA.setAtivo(true);
        cA = clienteRepository.save(cA);
        clienteIdA = cA.getId();

        Cliente cB = new Cliente();
        cB.setNome("Cliente B");
        cB.setEmail("cli-b-" + s + "@test.local");
        cB.setTenantId(t2.getId());
        cB.setAtivo(true);
        cB = clienteRepository.save(cB);
        clienteIdB = cB.getId();

        Produto pA = new Produto();
        pA.setNome("Prod A " + s);
        pA.setDescricao("d");
        pA.setPreco(10.0);
        pA.setQuantidade(50);
        pA.setAtivo(true);
        pA.setTenantId(t1.getId());
        pA = produtoRepository.save(pA);
        produtoIdA = pA.getId();

        Produto pB = new Produto();
        pB.setNome("Prod B " + s);
        pB.setDescricao("d");
        pB.setPreco(20.0);
        pB.setQuantidade(50);
        pB.setAtivo(true);
        pB.setTenantId(t2.getId());
        produtoRepository.save(pB);

        jwtTenantA = tokenService.gerarToken(uA);
        jwtTenantB = tokenService.gerarToken(uB);
    }

    private HttpResponse<String> get(String pathQuery, String jwt) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl() + pathQuery))
                .header("Authorization", "Bearer " + jwt)
                .GET()
                .build();
        return HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> postJson(String path, String json, String jwt) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .header("Authorization", "Bearer " + jwt)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        return HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> putJson(String path, String json, String jwt) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .header("Authorization", "Bearer " + jwt)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        return HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @Test
    void clientesIsoladosPorTenant() throws Exception {
        HttpResponse<String> list = get("/api/clientes", jwtTenantA);
        assertEquals(HttpStatus.OK.value(), list.statusCode());
        JsonNode arr = objectMapper.readTree(list.body());
        assertEquals(1, arr.size());
        assertTrue(arr.get(0).get("email").asText().contains("cli-a-"));

        HttpResponse<String> one = get("/api/clientes/" + clienteIdB, jwtTenantA);
        assertEquals(HttpStatus.NOT_FOUND.value(), one.statusCode());
    }

    @Test
    void produtosIsoladosPorTenant() throws Exception {
        HttpResponse<String> list = get("/api/produtos", jwtTenantB);
        assertEquals(HttpStatus.OK.value(), list.statusCode());
        JsonNode arr = objectMapper.readTree(list.body());
        assertEquals(1, arr.size());
        assertTrue(arr.get(0).get("nome").asText().contains("Prod B"));

        HttpResponse<String> one = get("/api/produtos/" + produtoIdA, jwtTenantB);
        assertEquals(HttpStatus.NOT_FOUND.value(), one.statusCode());
    }

    @Test
    void pedidosNaoAceitamClienteDeOutroTenant() throws Exception {
        ItemPedidoRequestDTO item = new ItemPedidoRequestDTO();
        item.setProdutoId(produtoIdA);
        item.setQuantidade(1);
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setClienteId(clienteIdB);
        dto.setItens(List.of(item));

        HttpResponse<String> resp = postJson("/api/pedidos", objectMapper.writeValueAsString(dto), jwtTenantA);
        assertEquals(HttpStatus.BAD_REQUEST.value(), resp.statusCode());
    }

    @Test
    void pedidosListagemSoDoTenant() throws Exception {
        ItemPedidoRequestDTO itemA = new ItemPedidoRequestDTO();
        itemA.setProdutoId(produtoIdA);
        itemA.setQuantidade(1);
        PedidoRequestDTO dtoA = new PedidoRequestDTO();
        dtoA.setClienteId(clienteIdA);
        dtoA.setItens(List.of(itemA));

        HttpResponse<String> created = postJson("/api/pedidos", objectMapper.writeValueAsString(dtoA), jwtTenantA);
        assertEquals(HttpStatus.CREATED.value(), created.statusCode());

        HttpResponse<String> listA = get("/api/pedidos", jwtTenantA);
        assertEquals(HttpStatus.OK.value(), listA.statusCode());
        assertEquals(1, objectMapper.readTree(listA.body()).size());

        HttpResponse<String> listB = get("/api/pedidos", jwtTenantB);
        assertEquals(HttpStatus.OK.value(), listB.statusCode());
        assertEquals(0, objectMapper.readTree(listB.body()).size());
    }

    @Test
    void usuariosListagemEAtualizacaoRespeitamTenant() throws Exception {
        HttpResponse<String> list = get("/api/usuarios", jwtTenantA);
        assertEquals(HttpStatus.OK.value(), list.statusCode());
        JsonNode arr = objectMapper.readTree(list.body());
        assertEquals(1, arr.size());
        assertTrue(arr.get(0).get("login").asText().contains("mt-a-"));

        String body = "{\"login\":\"mt-b-ignored@test.local\",\"username\":\"x\",\"role\":\"ADMIN\"}";
        HttpResponse<String> put = putJson("/api/usuarios/" + usuarioIdB, body, jwtTenantA);
        assertEquals(HttpStatus.NOT_FOUND.value(), put.statusCode());
    }

    @Test
    void dashboardExecutivoContaSoRecursosDoTenant() throws Exception {
        HttpResponse<String> dashA = get("/api/dashboard/executivo?periodo=30d", jwtTenantA);
        assertEquals(HttpStatus.OK.value(), dashA.statusCode());
        JsonNode rootA = objectMapper.readTree(dashA.body());
        assertEquals(1, rootA.get("totalClientes").asInt());
        assertEquals(1, rootA.get("totalProdutos").asInt());

        HttpResponse<String> dashB = get("/api/dashboard/executivo?periodo=30d", jwtTenantB);
        assertEquals(HttpStatus.OK.value(), dashB.statusCode());
        JsonNode rootB = objectMapper.readTree(dashB.body());
        assertEquals(1, rootB.get("totalClientes").asInt());
        assertEquals(1, rootB.get("totalProdutos").asInt());
    }
}

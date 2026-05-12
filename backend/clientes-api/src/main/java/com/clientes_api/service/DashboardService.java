package com.clientes_api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clientes_api.dto.dashboard.DashboardExecutivoResponseDTO;
import com.clientes_api.dto.dashboard.DashboardMetaConfigRequestDTO;
import com.clientes_api.dto.dashboard.DashboardMetaConfigResponseDTO;
import com.clientes_api.model.DashboardMetaConfig;
import com.clientes_api.model.ItemPedido;
import com.clientes_api.model.Pedido;
import com.clientes_api.model.Usuario;
import com.clientes_api.repository.ClienteRepository;
import com.clientes_api.repository.DashboardMetaConfigRepository;
import com.clientes_api.repository.PedidoRepository;
import com.clientes_api.repository.ProdutoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final DashboardMetaConfigRepository metaConfigRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardExecutivo", key = "T(java.lang.String).format('%s|%s|%s|%s', #tenantId, #login, #role, #periodo)")
    public DashboardExecutivoResponseDTO montarResumoExecutivo(String periodo, Long tenantId, String login, String role) {
        Intervalo intervalo = calcularIntervalo(periodo);
        DashboardExecutivoResponseDTO dto = new DashboardExecutivoResponseDTO();
        dto.setPeriodo(periodo);

        double metaReceita = resolverMetaReceita(login, role);
        dto.setMetaReceita(metaReceita);

        List<Pedido> todosPedidos = pedidoRepository.findAll();
        List<Pedido> pedidosPeriodo = filtrarPedidosPorPeriodo(todosPedidos, intervalo.inicioAtual, intervalo.fimAtual);
        List<Pedido> pedidosAnterior = filtrarPedidosPorPeriodo(todosPedidos, intervalo.inicioAnterior, intervalo.inicioAtual.minusNanos(1));

        dto.setTotalClientes((int) clienteRepository.count());
        dto.setClientesAtivos((int) clienteRepository.findAll().stream().filter(c -> c.getAtivo() == null || c.getAtivo()).count());
        dto.setTotalProdutos((int) produtoRepository.count());
        dto.setProdutosAtivos((int) produtoRepository.findAll().stream().filter(p -> p.getAtivo() == null || p.getAtivo()).count());
        dto.setProdutosBaixoEstoque((int) produtoRepository.findAll().stream().filter(p -> {
            int qtd = p.getQuantidade() != null ? p.getQuantidade() : 0;
            return qtd > 0 && qtd <= 10;
        }).count());

        dto.setTotalPedidos(pedidosPeriodo.size());
        dto.setPedidosAbertos((int) pedidosPeriodo.stream().filter(p -> "ABERTO".equalsIgnoreCase(p.getStatus().name())).count());

        double faturamentoAtual = pedidosPeriodo.stream().mapToDouble(p -> p.getValorTotal() != null ? p.getValorTotal() : 0.0).sum();
        double faturamentoAnterior = pedidosAnterior.stream().mapToDouble(p -> p.getValorTotal() != null ? p.getValorTotal() : 0.0).sum();

        dto.setFaturamentoTotal(faturamentoAtual);
        dto.setFaturamentoPeriodoAnterior(faturamentoAnterior);
        dto.setPedidosPeriodoAnterior(pedidosAnterior.size());
        dto.setTicketMedio(pedidosPeriodo.isEmpty() ? 0.0 : faturamentoAtual / pedidosPeriodo.size());

        dto.setPedidosRecentes(mapearPedidosRecentes(todosPedidos));
        dto.setTopClientes(mapearTopClientes(pedidosPeriodo));
        dto.setTopProdutos(mapearTopProdutos(pedidosPeriodo));
        dto.setTopVariacoes(mapearTopVariacoes(pedidosPeriodo, pedidosAnterior));

        dto.setDiasSemVenda(calcularDiasSemVenda(todosPedidos));
        dto.setSemaforoReceita(calcularSemaforoReceita(faturamentoAtual, metaReceita));
        dto.setSemaforoEstoque(calcularSemaforoEstoque(dto.getProdutosBaixoEstoque(), dto.getTotalProdutos()));
        dto.setSemaforoPedidos(calcularSemaforoPedidos(dto.getPedidosAbertos()));
        dto.setAlertasExecutivos(montarAlertas(dto));
        dto.setSerieReceitaDespesa(montarSerieReceitaDespesa(pedidosPeriodo, intervalo.inicioAtual(), intervalo.fimAtual()));
        dto.setVendasPorCategoria(montarVendasPorCategoria(pedidosPeriodo));

        return dto;
    }

    @Transactional(readOnly = true)
    public DashboardMetaConfigResponseDTO buscarMetaConfigurada() {
        Usuario usuario = usuarioLogado();
        double meta = resolverMetaReceita(usuario.getLogin(), usuario.getRole().name());

        DashboardMetaConfig cfgUser = metaConfigRepository.findFirstByLoginOrderByIdDesc(usuario.getLogin()).orElse(null);
        if (cfgUser != null) {
            return new DashboardMetaConfigResponseDTO("USER", usuario.getLogin(), cfgUser.getMetaReceita());
        }
        DashboardMetaConfig cfgRole = metaConfigRepository.findFirstByRoleOrderByIdDesc(usuario.getRole().name()).orElse(null);
        if (cfgRole != null) {
            return new DashboardMetaConfigResponseDTO("ROLE", usuario.getRole().name(), cfgRole.getMetaReceita());
        }
        return new DashboardMetaConfigResponseDTO("GLOBAL", "TENANT", meta);
    }

    @Transactional
    @CacheEvict(value = "dashboardExecutivo", allEntries = true)
    public DashboardMetaConfigResponseDTO salvarMeta(DashboardMetaConfigRequestDTO request) {
        if (request.getMetaReceita() == null || request.getMetaReceita() <= 0) {
            throw new RuntimeException("Meta de receita deve ser maior que zero");
        }
        Usuario usuario = usuarioLogado();
        String escopo = request.getEscopo() != null ? request.getEscopo().toUpperCase() : "USER";

        if ("ROLE".equals(escopo)) {
            String role = (request.getAlvo() == null || request.getAlvo().isBlank()) ? usuario.getRole().name() : request.getAlvo().toUpperCase();
            DashboardMetaConfig cfg = metaConfigRepository.findFirstByLoginIsNullAndRoleOrderByIdDesc(role).orElse(new DashboardMetaConfig());
            cfg.setLogin(null);
            cfg.setRole(role);
            cfg.setMetaReceita(request.getMetaReceita());
            metaConfigRepository.save(cfg);
            return new DashboardMetaConfigResponseDTO("ROLE", role, cfg.getMetaReceita());
        }

        if ("GLOBAL".equals(escopo)) {
            DashboardMetaConfig cfg = metaConfigRepository.findFirstByLoginIsNullAndRoleIsNullOrderByIdDesc().orElse(new DashboardMetaConfig());
            cfg.setLogin(null);
            cfg.setRole(null);
            cfg.setMetaReceita(request.getMetaReceita());
            metaConfigRepository.save(cfg);
            return new DashboardMetaConfigResponseDTO("GLOBAL", "TENANT", cfg.getMetaReceita());
        }

        DashboardMetaConfig cfg = metaConfigRepository.findFirstByLoginOrderByIdDesc(usuario.getLogin()).orElse(new DashboardMetaConfig());
        cfg.setLogin(usuario.getLogin());
        cfg.setRole(null);
        cfg.setMetaReceita(request.getMetaReceita());
        metaConfigRepository.save(cfg);
        return new DashboardMetaConfigResponseDTO("USER", usuario.getLogin(), cfg.getMetaReceita());
    }

    @CacheEvict(value = "dashboardExecutivo", allEntries = true)
    public void limparCacheDashboard() {
    }

    private Usuario usuarioLogado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Usuario usuario) {
            return usuario;
        }
        throw new RuntimeException("Usuário não autenticado");
    }

    private double resolverMetaReceita(String login, String role) {
        return metaConfigRepository.findFirstByLoginOrderByIdDesc(login).map(DashboardMetaConfig::getMetaReceita)
                .or(() -> metaConfigRepository.findFirstByRoleOrderByIdDesc(role).map(DashboardMetaConfig::getMetaReceita))
                .or(() -> metaConfigRepository.findFirstByLoginIsNullAndRoleIsNullOrderByIdDesc().map(DashboardMetaConfig::getMetaReceita))
                .orElse(50000.0);
    }

    private List<Pedido> filtrarPedidosPorPeriodo(List<Pedido> pedidos, LocalDateTime inicio, LocalDateTime fim) {
        return pedidos.stream().filter(p -> p.getDataPedido() != null && !p.getDataPedido().isBefore(inicio) && !p.getDataPedido().isAfter(fim)).toList();
    }

    private List<DashboardExecutivoResponseDTO.PedidoResumoDTO> mapearPedidosRecentes(List<Pedido> pedidos) {
        return pedidos.stream()
                .sorted(Comparator.comparing(Pedido::getDataPedido, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .map(p -> {
                    DashboardExecutivoResponseDTO.PedidoResumoDTO item = new DashboardExecutivoResponseDTO.PedidoResumoDTO();
                    item.setId(p.getId());
                    item.setCliente(p.getCliente() != null ? p.getCliente().getNome() : "Cliente não informado");
                    item.setStatus(p.getStatus() != null ? p.getStatus().name() : "N/A");
                    item.setValorTotal(p.getValorTotal());
                    item.setDataPedido(p.getDataPedido());
                    return item;
                }).toList();
    }

    private List<DashboardExecutivoResponseDTO.NomeValorDTO> mapearTopClientes(List<Pedido> pedidosPeriodo) {
        Map<String, Double> clientesMap = new HashMap<>();
        pedidosPeriodo.forEach(p -> {
            String nome = p.getCliente() != null ? p.getCliente().getNome() : "Cliente não informado";
            clientesMap.put(nome, clientesMap.getOrDefault(nome, 0.0) + (p.getValorTotal() != null ? p.getValorTotal() : 0.0));
        });
        return clientesMap.entrySet().stream()
                .map(e -> {
                    DashboardExecutivoResponseDTO.NomeValorDTO item = new DashboardExecutivoResponseDTO.NomeValorDTO();
                    item.setNome(e.getKey());
                    item.setTotal(e.getValue());
                    return item;
                })
                .sorted((a, b) -> Double.compare(b.getTotal(), a.getTotal()))
                .limit(5)
                .toList();
    }

    private List<DashboardExecutivoResponseDTO.NomeQuantidadeDTO> mapearTopProdutos(List<Pedido> pedidosPeriodo) {
        Map<String, Integer> produtosMap = new HashMap<>();
        pedidosPeriodo.forEach(p -> p.getItens().forEach(i -> {
            String nome = i.getProduto() != null ? i.getProduto().getNome() : "Produto não informado";
            int qtd = i.getQuantidade() != null ? i.getQuantidade() : 0;
            produtosMap.put(nome, produtosMap.getOrDefault(nome, 0) + qtd);
        }));
        return produtosMap.entrySet().stream()
                .map(e -> {
                    DashboardExecutivoResponseDTO.NomeQuantidadeDTO item = new DashboardExecutivoResponseDTO.NomeQuantidadeDTO();
                    item.setNome(e.getKey());
                    item.setQuantidade(e.getValue());
                    return item;
                })
                .sorted((a, b) -> Integer.compare(b.getQuantidade(), a.getQuantidade()))
                .limit(5)
                .toList();
    }

    private List<DashboardExecutivoResponseDTO.ProdutoVariacaoDTO> mapearTopVariacoes(List<Pedido> atual, List<Pedido> anterior) {
        Map<String, Integer> atualMap = new HashMap<>();
        Map<String, Integer> anteriorMap = new HashMap<>();

        atual.forEach(p -> p.getItens().forEach(i -> {
            String nome = i.getProduto() != null ? i.getProduto().getNome() : "Produto não informado";
            atualMap.put(nome, atualMap.getOrDefault(nome, 0) + (i.getQuantidade() != null ? i.getQuantidade() : 0));
        }));
        anterior.forEach(p -> p.getItens().forEach(i -> {
            String nome = i.getProduto() != null ? i.getProduto().getNome() : "Produto não informado";
            anteriorMap.put(nome, anteriorMap.getOrDefault(nome, 0) + (i.getQuantidade() != null ? i.getQuantidade() : 0));
        }));

        List<DashboardExecutivoResponseDTO.ProdutoVariacaoDTO> variacoes = new ArrayList<>();
        atualMap.keySet().forEach(nome -> {
            int atualQtd = atualMap.getOrDefault(nome, 0);
            int anteriorQtd = anteriorMap.getOrDefault(nome, 0);
            int delta = atualQtd - anteriorQtd;
            DashboardExecutivoResponseDTO.ProdutoVariacaoDTO item = new DashboardExecutivoResponseDTO.ProdutoVariacaoDTO();
            item.setProduto(nome);
            item.setAtual(atualQtd);
            item.setAnterior(anteriorQtd);
            item.setDelta(delta);
            item.setTendencia(delta > 0 ? "SUBIU" : delta < 0 ? "CAIU" : "ESTAVEL");
            variacoes.add(item);
        });

        anteriorMap.keySet().stream().filter(nome -> !atualMap.containsKey(nome)).forEach(nome -> {
            DashboardExecutivoResponseDTO.ProdutoVariacaoDTO item = new DashboardExecutivoResponseDTO.ProdutoVariacaoDTO();
            item.setProduto(nome);
            item.setAtual(0);
            item.setAnterior(anteriorMap.getOrDefault(nome, 0));
            item.setDelta(-anteriorMap.getOrDefault(nome, 0));
            item.setTendencia("CAIU");
            variacoes.add(item);
        });

        return variacoes.stream()
                .sorted((a, b) -> Integer.compare(Math.abs(b.getDelta()), Math.abs(a.getDelta())))
                .limit(6)
                .toList();
    }

    private int calcularDiasSemVenda(List<Pedido> pedidos) {
        return pedidos.stream()
                .map(Pedido::getDataPedido)
                .filter(d -> d != null)
                .max(LocalDateTime::compareTo)
                .map(ultimaData -> (int) ChronoUnit.DAYS.between(ultimaData.toLocalDate(), LocalDate.now()))
                .orElse(999);
    }

    private String calcularSemaforoReceita(double receita, double meta) {
        if (meta <= 0) return "VERDE";
        double progresso = receita / meta;
        if (progresso >= 1) return "VERDE";
        if (progresso >= 0.7) return "AMARELO";
        return "VERMELHO";
    }

    private String calcularSemaforoEstoque(int baixoEstoque, int totalProdutos) {
        if (totalProdutos <= 0 || baixoEstoque == 0) return "VERDE";
        double taxa = (double) baixoEstoque / totalProdutos;
        if (taxa <= 0.1) return "AMARELO";
        return "VERMELHO";
    }

    private String calcularSemaforoPedidos(int pedidosAbertos) {
        if (pedidosAbertos <= 5) return "VERDE";
        if (pedidosAbertos <= 15) return "AMARELO";
        return "VERMELHO";
    }

    private List<String> montarAlertas(DashboardExecutivoResponseDTO dto) {
        List<String> alertas = new ArrayList<>();
        if (dto.getProdutosBaixoEstoque() > 0) {
            alertas.add(dto.getProdutosBaixoEstoque() + " produto(s) com baixo estoque");
        }
        if (dto.getPedidosAbertos() > 0) {
            alertas.add(dto.getPedidosAbertos() + " pedido(s) em aberto");
        }
        if (dto.getTotalPedidos() == 0) {
            alertas.add("Nenhum pedido no período selecionado");
        }
        if (dto.getDiasSemVenda() > 3 && dto.getDiasSemVenda() < 999) {
            alertas.add(dto.getDiasSemVenda() + " dias sem nova venda");
        }
        return alertas;
    }

    private Intervalo calcularIntervalo(String periodo) {
        LocalDateTime fimAtual = LocalDateTime.now();
        LocalDateTime inicioAtual;
        LocalDateTime inicioAnterior;
        String p = periodo == null ? "30d" : periodo.toLowerCase();

        if ("hoje".equals(p)) {
            inicioAtual = LocalDate.now().atStartOfDay();
            inicioAnterior = inicioAtual.minusDays(1);
            return new Intervalo(inicioAtual, fimAtual, inicioAnterior);
        }
        if ("mes".equals(p)) {
            inicioAtual = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            inicioAnterior = inicioAtual.minusMonths(1);
            return new Intervalo(inicioAtual, fimAtual, inicioAnterior);
        }
        long dias = "7d".equals(p) ? 7 : 30;
        inicioAtual = LocalDate.now().minusDays(dias).atStartOfDay();
        inicioAnterior = inicioAtual.minusDays(dias);
        return new Intervalo(inicioAtual, fimAtual, inicioAnterior);
    }

    private List<DashboardExecutivoResponseDTO.SerieDiaDTO> montarSerieReceitaDespesa(
            List<Pedido> pedidosPeriodo, LocalDateTime inicioAtual, LocalDateTime fimAtual) {
        LocalDate start = inicioAtual.toLocalDate();
        LocalDate end = fimAtual.toLocalDate();
        LocalDate hoje = LocalDate.now();
        if (end.isAfter(hoje)) {
            end = hoje;
        }
        Map<LocalDate, Double> receitaPorDia = new HashMap<>();
        for (Pedido p : pedidosPeriodo) {
            if (p.getDataPedido() == null) {
                continue;
            }
            LocalDate d = p.getDataPedido().toLocalDate();
            double v = p.getValorTotal() != null ? p.getValorTotal() : 0.0;
            receitaPorDia.merge(d, v, Double::sum);
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        List<DashboardExecutivoResponseDTO.SerieDiaDTO> serie = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            double receita = receitaPorDia.getOrDefault(d, 0.0);
            DashboardExecutivoResponseDTO.SerieDiaDTO ponto = new DashboardExecutivoResponseDTO.SerieDiaDTO();
            ponto.setDia(d.format(fmt));
            ponto.setReceita(receita);
            // Estimativa operacional para comparação visual (não substitui contas a pagar / CMV real).
            ponto.setDespesa(receita * 0.62);
            serie.add(ponto);
        }
        return serie;
    }

    private List<DashboardExecutivoResponseDTO.CategoriaVendaDTO> montarVendasPorCategoria(List<Pedido> pedidosPeriodo) {
        Map<String, Double> porProduto = new HashMap<>();
        for (Pedido p : pedidosPeriodo) {
            if (p.getItens() == null) {
                continue;
            }
            for (ItemPedido i : p.getItens()) {
                String nome = i.getProduto() != null && i.getProduto().getNome() != null
                        ? i.getProduto().getNome()
                        : "Sem categoria";
                double v = 0.0;
                if (i.getSubtotal() != null) {
                    v = i.getSubtotal();
                } else if (i.getValorUnitario() != null && i.getQuantidade() != null) {
                    v = i.getValorUnitario() * i.getQuantidade();
                }
                porProduto.merge(nome, v, Double::sum);
            }
        }
        if (porProduto.isEmpty()) {
            return List.of();
        }
        double total = porProduto.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) {
            return List.of();
        }
        List<Map.Entry<String, Double>> ordenado = new ArrayList<>(porProduto.entrySet());
        ordenado.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        final int maxFatiasDiretas = 3;
        List<DashboardExecutivoResponseDTO.CategoriaVendaDTO> fatias = new ArrayList<>();
        double outros = 0.0;
        for (int i = 0; i < ordenado.size(); i++) {
            Map.Entry<String, Double> e = ordenado.get(i);
            if (i < maxFatiasDiretas) {
                DashboardExecutivoResponseDTO.CategoriaVendaDTO fatia = new DashboardExecutivoResponseDTO.CategoriaVendaDTO();
                fatia.setCategoria(truncarLabel(e.getKey(), 28));
                fatia.setValor(e.getValue());
                fatia.setPercentual(Math.round((e.getValue() / total) * 1000.0) / 10.0);
                fatias.add(fatia);
            } else {
                outros += e.getValue();
            }
        }
        if (outros > 0.01) {
            DashboardExecutivoResponseDTO.CategoriaVendaDTO fatia = new DashboardExecutivoResponseDTO.CategoriaVendaDTO();
            fatia.setCategoria("Outros");
            fatia.setValor(outros);
            fatia.setPercentual(Math.round((outros / total) * 1000.0) / 10.0);
            fatias.add(fatia);
        }
        double somaPct = fatias.stream().mapToDouble(DashboardExecutivoResponseDTO.CategoriaVendaDTO::getPercentual).sum();
        if (!fatias.isEmpty() && Math.abs(somaPct - 100.0) > 0.5) {
            DashboardExecutivoResponseDTO.CategoriaVendaDTO ultima = fatias.get(fatias.size() - 1);
            ultima.setPercentual(Math.round((ultima.getPercentual() + (100.0 - somaPct)) * 10.0) / 10.0);
        }
        return fatias;
    }

    private static String truncarLabel(String texto, int max) {
        if (texto == null || texto.isBlank()) {
            return "—";
        }
        String t = texto.trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }

    private record Intervalo(LocalDateTime inicioAtual, LocalDateTime fimAtual, LocalDateTime inicioAnterior) {
    }
}

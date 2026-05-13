package com.clientes_api.dto.dashboard;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class DashboardExecutivoResponseDTO {
    private String periodo;
    private Double metaReceita;

    private Integer totalClientes;
    private Integer clientesAtivos;
    private Integer totalProdutos;
    private Integer produtosAtivos;
    private Integer produtosBaixoEstoque;
    private Integer totalPedidos;
    private Integer pedidosAbertos;
    private Integer diasSemVenda;

    private Double faturamentoTotal;
    private Double faturamentoPeriodoAnterior;
    private Integer pedidosPeriodoAnterior;
    private Double ticketMedio;

    private String semaforoReceita;
    private String semaforoEstoque;
    private String semaforoPedidos;

    private List<String> alertasExecutivos = new ArrayList<>();
    private List<NomeValorDTO> topClientes = new ArrayList<>();
    private List<NomeQuantidadeDTO> topProdutos = new ArrayList<>();
    private List<ProdutoVariacaoDTO> topVariacoes = new ArrayList<>();
    private List<PedidoResumoDTO> pedidosRecentes = new ArrayList<>();

    /** Receita diária no período + despesa estimada (proxy operacional para visualização). */
    private List<SerieDiaDTO> serieReceitaDespesa = new ArrayList<>();

    /** Fatia de vendas por agrupamento de produto (top N + Outros), com base no faturamento dos itens. */
    private List<CategoriaVendaDTO> vendasPorCategoria = new ArrayList<>();

    @Data
    public static class SerieDiaDTO {
        private String dia;
        private Double receita;
        /** Estimativa para gráfico executivo (não substitui módulo financeiro). */
        private Double despesa;
    }

    @Data
    public static class CategoriaVendaDTO {
        private String categoria;
        private Double valor;
        private Double percentual;
    }

    @Data
    public static class NomeValorDTO {
        private String nome;
        private Double total;
    }

    @Data
    public static class NomeQuantidadeDTO {
        private String nome;
        private Integer quantidade;
    }

    @Data
    public static class ProdutoVariacaoDTO {
        private String produto;
        private Integer atual;
        private Integer anterior;
        private Integer delta;
        private String tendencia;
    }

    @Data
    public static class PedidoResumoDTO {
        private Long id;
        private String cliente;
        private String status;
        private Double valorTotal;
        private LocalDateTime dataPedido;
    }
}

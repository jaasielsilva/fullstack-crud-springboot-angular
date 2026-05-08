import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { DashboardService, DashboardExecutivoResponse } from './dashboard.service';

interface SparkPoint {
  x: number;
  y: number;
  value: number;
  label: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule], // Adicionamos aqui!
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  periodoSelecionado: '7d' | '30d' | 'mes' | 'hoje' = '30d';

  totalClientes = 0;
  clientesAtivos = 0;
  totalProdutos = 0;
  produtosAtivos = 0;
  produtosBaixoEstoque = 0;
  totalPedidos = 0;
  pedidosAbertos = 0;
  faturamentoTotal = 0;
  ticketMedio = 0;
  faturamentoPeriodoAnterior = 0;
  pedidosPeriodoAnterior = 0;
  semaforoReceita: 'VERDE' | 'AMARELO' | 'VERMELHO' = 'VERDE';
  semaforoEstoque: 'VERDE' | 'AMARELO' | 'VERMELHO' = 'VERDE';
  semaforoPedidos: 'VERDE' | 'AMARELO' | 'VERMELHO' = 'VERDE';

  topClientes: Array<{ nome: string; total: number }> = [];
  topProdutos: Array<{ nome: string; quantidade: number }> = [];
  topVariacoes: Array<{ produto: string; atual: number; anterior: number; delta: number; tendencia: string }> = [];
  pedidosRecentes: Array<{ id: number; cliente: string; status: string; valorTotal: number; dataPedido: string }> = [];
  alertasExecutivos: string[] = [];
  receitaSerie: number[] = [];
  pedidosSerie: number[] = [];
  receitaSpark: SparkPoint[] = [];
  pedidosSpark: SparkPoint[] = [];
  diasSemVenda = 0;
  metaReceita = 50000;

  carregando = true;
  erroCarregamento = '';
  metaEscopo = '';
  metaAlvo = '';

  constructor(private dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.carregarDashboardExecutivo();
  }

  selecionarPeriodo(periodo: '7d' | '30d' | 'mes' | 'hoje'): void {
    this.periodoSelecionado = periodo;
    this.carregarDashboardExecutivo();
  }

  carregarDashboardExecutivo(): void {
    this.carregando = true;
    this.erroCarregamento = '';
    this.dashboardService.resumoExecutivo(this.periodoSelecionado).subscribe({
      next: (data: DashboardExecutivoResponse) => {
        this.totalClientes = data.totalClientes;
        this.clientesAtivos = data.clientesAtivos;
        this.totalProdutos = data.totalProdutos;
        this.produtosAtivos = data.produtosAtivos;
        this.produtosBaixoEstoque = data.produtosBaixoEstoque;
        this.totalPedidos = data.totalPedidos;
        this.pedidosAbertos = data.pedidosAbertos;
        this.faturamentoTotal = data.faturamentoTotal;
        this.faturamentoPeriodoAnterior = data.faturamentoPeriodoAnterior;
        this.pedidosPeriodoAnterior = data.pedidosPeriodoAnterior;
        this.ticketMedio = data.ticketMedio;
        this.diasSemVenda = data.diasSemVenda;
        this.metaReceita = data.metaReceita;
        this.semaforoReceita = data.semaforoReceita;
        this.semaforoEstoque = data.semaforoEstoque;
        this.semaforoPedidos = data.semaforoPedidos;
        this.topClientes = data.topClientes ?? [];
        this.topProdutos = data.topProdutos ?? [];
        this.topVariacoes = data.topVariacoes ?? [];
        this.pedidosRecentes = data.pedidosRecentes ?? [];
        this.alertasExecutivos = data.alertasExecutivos ?? [];

        this.montarSeriesPeriodo(this.pedidosRecentes);
        this.carregando = false;
      },
      error: (_err: HttpErrorResponse) => {
        this.erroCarregamento = 'Não foi possível carregar o dashboard executivo.';
        this.carregando = false;
      }
    });

    this.dashboardService.buscarMeta().subscribe({
      next: (meta) => {
        this.metaEscopo = meta.escopo;
        this.metaAlvo = meta.alvo;
      }
    });
  }

  sparklinePoints(serie: number[], width = 120, height = 34): string {
    if (!serie.length) return `0,${height} ${width},${height}`;
    const max = Math.max(...serie, 1);
    const min = Math.min(...serie, 0);
    const range = Math.max(max - min, 1);
    const step = serie.length > 1 ? width / (serie.length - 1) : width;

    return serie
      .map((valor, i) => {
        const x = i * step;
        const y = height - ((valor - min) / range) * height;
        return `${x.toFixed(2)},${y.toFixed(2)}`;
      })
      .join(' ');
  }

  progressoMetaReceita(): number {
    if (this.metaReceita <= 0) return 0;
    return Math.min(100, (this.faturamentoTotal / this.metaReceita) * 100);
  }

  variacaoPercentual(atual: number, anterior: number): number {
    if (anterior <= 0) {
      return atual > 0 ? 100 : 0;
    }
    return ((atual - anterior) / anterior) * 100;
  }

  variacaoClasse(valor: number): 'trend-up' | 'trend-down' | 'trend-neutral' {
    if (valor > 0.1) return 'trend-up';
    if (valor < -0.1) return 'trend-down';
    return 'trend-neutral';
  }

  variacaoIcone(valor: number): string {
    if (valor > 0.1) return 'bi-arrow-up-right';
    if (valor < -0.1) return 'bi-arrow-down-right';
    return 'bi-dash';
  }

  labelPeriodo(): string {
    if (this.periodoSelecionado === '7d') return 'últimos 7 dias';
    if (this.periodoSelecionado === 'hoje') return 'hoje';
    if (this.periodoSelecionado === 'mes') return 'mês atual';
    return 'últimos 30 dias';
  }
  semaforoClasse(valor: 'VERDE' | 'AMARELO' | 'VERMELHO'): string {
    if (valor === 'VERDE') return 'semaforo-verde';
    if (valor === 'AMARELO') return 'semaforo-amarelo';
    return 'semaforo-vermelho';
  }

  exportarResumoPdf(): void {
    import('jspdf').then(({ default: jsPDF }) => {
      const pdf = new jsPDF({ unit: 'pt', format: 'a4' });
      let y = 50;
      const addLine = (label: string, value: string) => {
        pdf.setFont('helvetica', 'bold');
        pdf.text(`${label}:`, 40, y);
        pdf.setFont('helvetica', 'normal');
        pdf.text(value, 170, y);
        y += 20;
      };

      pdf.setFontSize(16);
      pdf.setFont('helvetica', 'bold');
      pdf.text('Resumo Executivo - Dashboard', 40, y);
      y += 28;
      pdf.setFontSize(11);
      addLine('Periodo', this.labelPeriodo());
      addLine('Receita total', this.faturamentoTotal.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }));
      addLine('Ticket medio', this.ticketMedio.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }));
      addLine('Meta de receita', this.metaReceita.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }));
      addLine('Total de pedidos', `${this.totalPedidos}`);
      addLine('Pedidos em aberto', `${this.pedidosAbertos}`);
      addLine('Produtos com baixo estoque', `${this.produtosBaixoEstoque}`);
      addLine('Dias sem venda', `${this.diasSemVenda}`);
      y += 10;
      pdf.setFont('helvetica', 'bold');
      pdf.text('Top variacoes de produtos', 40, y);
      y += 18;
      pdf.setFont('helvetica', 'normal');
      this.topVariacoes.slice(0, 6).forEach(item => {
        pdf.text(`${item.produto}: ${item.delta > 0 ? '+' : ''}${item.delta} un. (${item.tendencia})`, 50, y);
        y += 16;
      });

      pdf.save(`dashboard-executivo-${this.periodoSelecionado}.pdf`);
    });
  }

  private montarSeriesPeriodo(pedidosRecentes: Array<{ dataPedido: string; valorTotal: number }>): void {
    const ordenados = [...pedidosRecentes]
      .filter(p => !!p.dataPedido)
      .sort((a, b) => new Date(a.dataPedido).getTime() - new Date(b.dataPedido).getTime());

    const receita = ordenados.map(p => p.valorTotal || 0);
    const quantidade = ordenados.map(() => 1);
    const labels = ordenados.map(p => new Date(p.dataPedido).toLocaleDateString('pt-BR'));

    this.receitaSerie = receita.length ? receita : [0];
    this.pedidosSerie = quantidade.length ? quantidade : [0];
    this.receitaSpark = this.montarSparkData(this.receitaSerie, labels.length ? labels : ['']);
    this.pedidosSpark = this.montarSparkData(this.pedidosSerie, labels.length ? labels : ['']);
  }

  private montarSparkData(serie: number[], labels: string[], width = 120, height = 34): SparkPoint[] {
    if (!serie.length) {
      return [{ x: 0, y: height, value: 0, label: labels[0] ?? '' }];
    }
    const max = Math.max(...serie, 1);
    const min = Math.min(...serie, 0);
    const range = Math.max(max - min, 1);
    const step = serie.length > 1 ? width / (serie.length - 1) : width;

    return serie.map((value, index) => ({
      x: index * step,
      y: height - ((value - min) / range) * height,
      value,
      label: labels[index] ?? ''
    }));
  }

}

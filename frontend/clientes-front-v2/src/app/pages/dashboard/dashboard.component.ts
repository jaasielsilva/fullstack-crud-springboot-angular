import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { DashboardService, DashboardExecutivoResponse } from './dashboard.service';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit, OnDestroy {
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
  serieReceitaDespesa: Array<{ dia: string; receita: number; despesa: number }> = [];
  vendasPorCategoria: Array<{ categoria: string; valor: number; percentual: number }> = [];

  /** Snapshot ao carregar a API — evita NG0100 (Date.now() mudando entre verificações de CD). */
  atividadesRecentes: Array<{ icone: string; texto: string; tempo: string }> = [];
  intervaloLegenda = '';

  diasSemVenda = 0;
  metaReceita = 50000;

  carregando = true;
  erroCarregamento = '';
  metaEscopo = '';
  metaAlvo = '';

  private chartLinha?: Chart;
  private chartDonut?: Chart;

  constructor(private dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.carregarDashboardExecutivo();
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  selecionarPeriodo(periodo: '7d' | '30d' | 'mes' | 'hoje'): void {
    this.periodoSelecionado = periodo;
    this.carregarDashboardExecutivo();
  }

  carregarDashboardExecutivo(): void {
    this.destroyCharts();
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
        this.serieReceitaDespesa = data.serieReceitaDespesa ?? [];
        this.vendasPorCategoria = data.vendasPorCategoria ?? [];

        this.intervaloLegenda = this.calcularLegendaIntervaloDatas();
        this.atividadesRecentes = this.montarAtividadesSnapshot();

        this.carregando = false;
        setTimeout(() => this.renderCharts(), 0);
      },
      error: (_err: HttpErrorResponse) => {
        this.erroCarregamento = 'Não foi possível carregar o dashboard executivo.';
        this.carregando = false;
      }
    });

    this.dashboardService.buscarMeta().subscribe({
      next: meta => {
        this.metaEscopo = meta.escopo;
        this.metaAlvo = meta.alvo;
      }
    });
  }

  /** Preferir binding `intervaloLegenda` (atualizado no load); evita `new Date()` a cada CD. */
  calcularLegendaIntervaloDatas(): string {
    const fim = new Date();
    let inicio = new Date();
    if (this.periodoSelecionado === 'hoje') {
      inicio = new Date(fim.getFullYear(), fim.getMonth(), fim.getDate());
    } else if (this.periodoSelecionado === '7d') {
      inicio.setDate(fim.getDate() - 6);
    } else if (this.periodoSelecionado === 'mes') {
      inicio = new Date(fim.getFullYear(), fim.getMonth(), 1);
    } else {
      inicio.setDate(fim.getDate() - 29);
    }
    const fmt = (d: Date) =>
      d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
    return `${fmt(inicio)} — ${fmt(fim)}`;
  }

  variacaoPercentual(atual: number, anterior: number): number {
    const cur = Number(atual ?? 0);
    const prev = Number(anterior ?? 0);
    if (prev <= 0) {
      return cur > 0 ? 100 : 0;
    }
    return ((cur - prev) / prev) * 100;
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

  trackAtividade(_index: number, item: { texto: string }): string {
    return item.texto;
  }

  semaforoClasse(valor: 'VERDE' | 'AMARELO' | 'VERMELHO'): string {
    if (valor === 'VERDE') return 'semaforo-verde';
    if (valor === 'AMARELO') return 'semaforo-amarelo';
    return 'semaforo-vermelho';
  }

  labelStatus(status: string): string {
    const s = (status || '').toUpperCase();
    if (s === 'ENTREGUE') return 'Concluído';
    if (s === 'PAGO') return 'Processando';
    if (s === 'ABERTO') return 'Pendente';
    if (s === 'CANCELADO') return 'Cancelado';
    return status || '—';
  }

  badgeStatusClasse(status: string): string {
    const s = (status || '').toUpperCase();
    if (s === 'ENTREGUE') return 'badge-status-concluido';
    if (s === 'PAGO') return 'badge-status-processando';
    if (s === 'ABERTO') return 'badge-status-pendente';
    if (s === 'CANCELADO') return 'badge-status-cancelado';
    return 'badge-status-neutro';
  }

  tempoRelativo(dataIso: string): string {
    if (!dataIso) return '';
    const d = new Date(dataIso);
    if (Number.isNaN(d.getTime())) return '';
    const diffMs = Date.now() - d.getTime();
    const min = Math.floor(diffMs / 60000);
    if (min < 1) return 'agora';
    if (min < 60) return `há ${min} min`;
    const h = Math.floor(min / 60);
    if (h < 24) return `há ${h} h`;
    const days = Math.floor(h / 24);
    return `há ${days} dia(s)`;
  }

  private montarAtividadesSnapshot(): Array<{ icone: string; texto: string; tempo: string }> {
    const itens: Array<{ icone: string; texto: string; tempo: string }> = [];
    const recentes = [...this.pedidosRecentes].slice(0, 5);
    for (const p of recentes) {
      itens.push({
        icone: 'bi-bag-check',
        texto: `Pedido #${p.id} — ${this.labelStatus(p.status)}`,
        tempo: this.tempoRelativo(p.dataPedido)
      });
    }
    if (this.topClientes[0]) {
      itens.push({
        icone: 'bi-person-plus',
        texto: `Destaque em vendas — ${this.topClientes[0].nome}`,
        tempo: this.labelPeriodo()
      });
    }
    return itens.slice(0, 6);
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

  private destroyCharts(): void {
    this.chartLinha?.destroy();
    this.chartLinha = undefined;
    this.chartDonut?.destroy();
    this.chartDonut = undefined;
  }

  private renderCharts(): void {
    const elLinha = document.getElementById('chartReceitaDespesa') as HTMLCanvasElement | null;
    const elDonut = document.getElementById('chartCategorias') as HTMLCanvasElement | null;
    if (!elLinha || !elDonut) return;

    const labels = this.serieReceitaDespesa.map(p => p.dia);
    const receitas = this.serieReceitaDespesa.map(p => p.receita ?? 0);
    const despesas = this.serieReceitaDespesa.map(p => p.despesa ?? 0);

    const cfgLinha: ChartConfiguration<'line'> = {
      type: 'line',
      data: {
        labels: labels.length ? labels : ['—'],
        datasets: [
          {
            label: 'Receita',
            data: receitas.length ? receitas : [0],
            borderColor: '#2563eb',
            backgroundColor: 'rgba(37, 99, 235, 0.08)',
            fill: true,
            tension: 0.35,
            pointRadius: 0,
            borderWidth: 2
          },
          {
            label: 'Despesa (estimada)',
            data: despesas.length ? despesas : [0],
            borderColor: '#dc2626',
            backgroundColor: 'rgba(220, 38, 38, 0.06)',
            fill: true,
            tension: 0.35,
            pointRadius: 0,
            borderWidth: 2
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: {
          legend: { position: 'top', labels: { usePointStyle: true, boxWidth: 8 } },
          tooltip: {
            callbacks: {
              label: ctx => {
                const v = ctx.parsed.y as number;
                return `${ctx.dataset.label}: ${v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}`;
              }
            }
          }
        },
        scales: {
          x: { grid: { display: false }, ticks: { maxRotation: 0, autoSkip: true, maxTicksLimit: 12 } },
          y: {
            grid: { color: 'rgba(0,0,0,0.06)' },
            ticks: {
              callback: value =>
                Number(value).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })
            }
          }
        }
      }
    };
    this.chartLinha = new Chart(elLinha, cfgLinha);

    const cats = this.vendasPorCategoria;
    const donutLabels = cats.length ? cats.map(c => c.categoria) : ['Sem dados'];
    const donutData = cats.length ? cats.map(c => c.valor) : [1];
    const colors = ['#2563eb', '#38bdf8', '#fbbf24', '#f87171', '#94a3b8'];

    const cfgDonut: ChartConfiguration<'doughnut'> = {
      type: 'doughnut',
      data: {
        labels: donutLabels,
        datasets: [
          {
            data: donutData,
            backgroundColor: donutLabels.map((_, i) => colors[i % colors.length]),
            borderWidth: 0
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '68%',
        plugins: {
          legend: { position: 'bottom', labels: { usePointStyle: true, boxWidth: 8, padding: 12 } },
          tooltip: {
            callbacks: {
              label: ctx => {
                const raw = ctx.raw as number;
                const pct = cats[ctx.dataIndex]?.percentual;
                const p = pct != null ? ` (${pct}%)` : '';
                return `${ctx.label}: ${raw.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}${p}`;
              }
            }
          }
        }
      }
    };
    this.chartDonut = new Chart(elDonut, cfgDonut as never);
  }
}

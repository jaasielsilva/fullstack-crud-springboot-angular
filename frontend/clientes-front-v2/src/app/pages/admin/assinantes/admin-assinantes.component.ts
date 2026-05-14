import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminAssinantesService } from '../../../services/admin-assinantes.service';
import { AssinanteAdmin, AdminDashboardMetrics } from '../../../models/assinante-admin.model';
import Swal from 'sweetalert2';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-admin-assinantes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-assinantes.component.html',
  styleUrls: ['./admin-assinantes.component.css']
})
export class AdminAssinantesComponent implements OnInit {
  assinantes: AssinanteAdmin[] = [];
  assinantesFiltrados: AssinanteAdmin[] = [];
  metricas: AdminDashboardMetrics | null = null;
  carregando = true;
  termoBusca = '';
  filtroStatus = 'TODOS';
  exportandoCsv = false;

  constructor(private adminAssinantesService: AdminAssinantesService) {}

  ngOnInit(): void {
    this.carregarDados();
  }

  carregarDados(): void {
    this.carregando = true;
    
    // Obter Métricas
    this.adminAssinantesService.obterMetricas().subscribe({
      next: (dados) => {
        this.metricas = dados;
      },
      error: (err) => console.error('Erro ao carregar métricas:', err)
    });

    // Obter Assinantes
    this.adminAssinantesService.listarAssinantes().subscribe({
      next: (dados) => {
        this.assinantes = dados;
        this.aplicarFiltros();
        this.carregando = false;
      },
      error: (err) => {
        console.error('Erro ao carregar assinantes:', err);
        this.carregando = false;
        Swal.fire('Erro', 'Não foi possível carregar os assinantes', 'error');
      }
    });
  }

  aplicarFiltros(): void {
    this.assinantesFiltrados = this.assinantes.filter(a => {
      const atendeBusca = 
        !this.termoBusca || 
        a.nomeEmpresa.toLowerCase().includes(this.termoBusca.toLowerCase()) || 
        (a.documento && a.documento.includes(this.termoBusca)) ||
        (a.email && a.email.toLowerCase().includes(this.termoBusca.toLowerCase()));

      const atendeStatus =
        this.filtroStatus === 'TODOS' ||
        a.statusEmpresa === this.filtroStatus ||
        (this.filtroStatus === 'INADIMPLENTES' &&
          a.ultimoPagamentoStatus &&
          ['PENDING', 'REJECTED'].includes(a.ultimoPagamentoStatus)) ||
        (this.filtroStatus === 'RENOVACAO_7D' &&
          a.statusAssinatura === 'ATIVA' &&
          a.statusEmpresa === 'ATIVA' &&
          a.diasAteVencimentoPlano !== null &&
          a.diasAteVencimentoPlano !== undefined &&
          a.diasAteVencimentoPlano >= 0 &&
          a.diasAteVencimentoPlano <= 7);

      return atendeBusca && atendeStatus;
    });
  }

  alterarStatusEmpresa(assinante: AssinanteAdmin): void {
    const isBloqueada = assinante.statusEmpresa === 'BLOQUEADA';
    const acaoTexto = isBloqueada ? 'Liberar Acesso' : 'Bloquear Acesso';
    const novoStatus = isBloqueada ? 'ATIVA' : 'BLOQUEADA';
    const confirmButtonColor = isBloqueada ? '#10b981' : '#ef4444';

    Swal.fire({
      title: `${acaoTexto}?`,
      text: `Você tem certeza que deseja ${acaoTexto.toLowerCase()} para a empresa ${assinante.nomeEmpresa}?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor,
      cancelButtonColor: '#6b7280',
      confirmButtonText: `Sim, ${acaoTexto}!`,
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.adminAssinantesService.atualizarStatus(assinante.empresaId, novoStatus).subscribe({
          next: () => {
            Swal.fire('Sucesso!', `O status da empresa foi atualizado para ${novoStatus}.`, 'success');
            this.carregarDados();
          },
          error: (err) => {
            Swal.fire('Erro', 'Houve um erro ao atualizar o status.', 'error');
            console.error(err);
          }
        });
      }
    });
  }

  exportarCsv(): void {
    this.exportandoCsv = true;
    this.adminAssinantesService.exportarCsv().subscribe({
      next: (blob) => {
        this.exportandoCsv = false;
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `assinantes-lexcrm-${new Date().toISOString().slice(0, 10)}.csv`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.exportandoCsv = false;
        console.error(err);
        Swal.fire('Erro', 'Não foi possível exportar o CSV.', 'error');
      }
    });
  }

  verDetalhes(assinante: AssinanteAdmin): void {
    const detalhesHTML = `
      <div style="text-align: left; font-size: 0.95rem; line-height: 1.6;">
        <p><strong>Empresa:</strong> ${assinante.nomeEmpresa}</p>
        <p><strong>Documento:</strong> ${assinante.documento || 'Não informado'}</p>
        <p><strong>Email:</strong> ${assinante.email || 'Não informado'}</p>
        <hr style="margin: 10px 0; border: 0; border-top: 1px solid #eee;">
        <p><strong>Status Atual:</strong> ${assinante.statusEmpresa}</p>
        <p><strong>Plano:</strong> ${assinante.planoNome || 'Sem Plano'}</p>
        <p><strong>Assinatura:</strong> ${assinante.statusAssinatura || 'N/A'}</p>
        <p><strong>Vencimento:</strong> ${assinante.dataVencimento ? new Date(assinante.dataVencimento).toLocaleDateString('pt-BR') : 'N/A'}</p>
        <p><strong>Dias até vencimento (plano):</strong> ${
          assinante.diasAteVencimentoPlano !== null && assinante.diasAteVencimentoPlano !== undefined
            ? assinante.diasAteVencimentoPlano
            : 'N/A'
        }</p>
        <hr style="margin: 10px 0; border: 0; border-top: 1px solid #eee;">
        <p><strong>Último Pagamento:</strong> ${assinante.ultimoPagamentoStatus || 'N/A'}</p>
        <p><strong>Valor:</strong> ${assinante.valorUltimoPagamento ? 'R$ ' + assinante.valorUltimoPagamento : 'N/A'}</p>
        <p><strong>Data Pgto:</strong> ${assinante.ultimoPagamentoData ? new Date(assinante.ultimoPagamentoData).toLocaleDateString('pt-BR') : 'N/A'}</p>
      </div>
    `;

    Swal.fire({
      title: 'Detalhes do Assinante',
      html: detalhesHTML,
      confirmButtonText: 'Fechar',
      confirmButtonColor: '#3b82f6',
      width: '500px'
    });
  }
}

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { DashboardService } from '../dashboard/dashboard.service';
import { AuthService } from '../../security/auth.service';

@Component({
  selector: 'app-configuracoes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './configuracoes.component.html',
  styleUrl: './configuracoes.component.css'
})
export class ConfiguracoesComponent implements OnInit {
  
  emailConfig: any = {
    host: 'smtp.gmail.com',
    port: 587,
    usuario: '',
    senha: '',
    protocol: 'smtp',
    auth: true,
    starttls: true
  };

  carregando = false;
  mensagemSucesso = '';
  mensagemErro = '';
  abaAtiva = 'email'; // Controla qual aba de configuração está visível

  notificacoesConfig = {
    novosClientes: true,
    novosPedidos: true,
    relatoriosMensais: false,
    avisarEmail: true,
    avisarBrowser: true
  };

  dashboardMetaConfig: { escopo: 'USER' | 'ROLE' | 'GLOBAL'; alvo: string; metaReceita: number } = {
    escopo: 'ROLE',
    alvo: 'GERENTE',
    metaReceita: 50000
  };

  rolesDisponiveis = ['ADMIN', 'GERENTE', 'VENDEDOR', 'SUPORTE'];
  isAdmin = false;

  private apiUrl = `${environment.apiUrl}/api/config/email`;

  constructor(
    private http: HttpClient,
    private dashboardService: DashboardService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.authService.getRole() === 'ADMIN';
    this.carregarConfiguracao();
    this.carregarMetaDashboard();
  }

  carregarConfiguracao() {
    this.http.get(this.apiUrl).subscribe({
      next: (data: any) => {
        if (data) {
          this.emailConfig = data;
        }
      },
      error: (err) => {
        console.error('Erro ao carregar configurações', err);
      }
    });
  }

  salvarEmailConfig() {
    this.carregando = true;
    this.mensagemSucesso = '';
    this.mensagemErro = '';

    this.http.post(this.apiUrl, this.emailConfig).subscribe({
      next: (data: any) => {
        this.emailConfig = data;
        this.mensagemSucesso = 'Configurações de e-mail salvas com sucesso!';
        this.carregando = false;
        setTimeout(() => this.mensagemSucesso = '', 3000);
      },
      error: (err) => {
        this.mensagemErro = 'Erro ao salvar configurações de e-mail.';
        this.carregando = false;
        console.error(err);
      }
    });
  }

  carregarMetaDashboard(): void {
    this.dashboardService.buscarMeta().subscribe({
      next: (data) => {
        this.dashboardMetaConfig = {
          escopo: data.escopo,
          alvo: data.alvo,
          metaReceita: data.metaReceita
        };
      }
    });
  }

  salvarMetaDashboard(): void {
    this.carregando = true;
    this.mensagemSucesso = '';
    this.mensagemErro = '';

    this.dashboardService.salvarMeta({
      escopo: this.dashboardMetaConfig.escopo,
      alvo: this.dashboardMetaConfig.alvo,
      metaReceita: this.dashboardMetaConfig.metaReceita
    }).subscribe({
      next: (resp) => {
        this.dashboardMetaConfig = {
          escopo: resp.escopo,
          alvo: resp.alvo,
          metaReceita: resp.metaReceita
        };
        this.mensagemSucesso = 'Meta do dashboard salva com sucesso!';
        this.carregando = false;
      },
      error: () => {
        this.mensagemErro = 'Erro ao salvar meta do dashboard.';
        this.carregando = false;
      }
    });
  }
}

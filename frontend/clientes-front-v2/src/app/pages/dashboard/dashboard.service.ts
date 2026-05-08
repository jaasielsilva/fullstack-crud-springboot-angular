import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DashboardExecutivoResponse {
  periodo: string;
  metaReceita: number;
  totalClientes: number;
  clientesAtivos: number;
  totalProdutos: number;
  produtosAtivos: number;
  produtosBaixoEstoque: number;
  totalPedidos: number;
  pedidosAbertos: number;
  diasSemVenda: number;
  faturamentoTotal: number;
  faturamentoPeriodoAnterior: number;
  pedidosPeriodoAnterior: number;
  ticketMedio: number;
  semaforoReceita: 'VERDE' | 'AMARELO' | 'VERMELHO';
  semaforoEstoque: 'VERDE' | 'AMARELO' | 'VERMELHO';
  semaforoPedidos: 'VERDE' | 'AMARELO' | 'VERMELHO';
  alertasExecutivos: string[];
  topClientes: Array<{ nome: string; total: number }>;
  topProdutos: Array<{ nome: string; quantidade: number }>;
  topVariacoes: Array<{ produto: string; atual: number; anterior: number; delta: number; tendencia: string }>;
  pedidosRecentes: Array<{ id: number; cliente: string; status: string; valorTotal: number; dataPedido: string }>;
}

export interface DashboardMetaConfigResponse {
  escopo: 'USER' | 'ROLE' | 'GLOBAL';
  alvo: string;
  metaReceita: number;
}

export interface DashboardMetaConfigRequest {
  escopo: 'USER' | 'ROLE' | 'GLOBAL';
  alvo?: string;
  metaReceita: number;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private apiUrl = `${environment.apiUrl}/api/dashboard`;

  constructor(private http: HttpClient) {}

  resumoExecutivo(periodo: 'hoje' | '7d' | '30d' | 'mes'): Observable<DashboardExecutivoResponse> {
    return this.http.get<DashboardExecutivoResponse>(`${this.apiUrl}/executivo`, {
      params: { periodo }
    });
  }

  buscarMeta(): Observable<DashboardMetaConfigResponse> {
    return this.http.get<DashboardMetaConfigResponse>(`${this.apiUrl}/meta`);
  }

  salvarMeta(payload: DashboardMetaConfigRequest): Observable<DashboardMetaConfigResponse> {
    return this.http.post<DashboardMetaConfigResponse>(`${this.apiUrl}/meta`, payload);
  }
}

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AssinanteAdmin, AdminDashboardMetrics } from '../models/assinante-admin.model';

@Injectable({
  providedIn: 'root'
})
export class AdminAssinantesService {
  private apiUrl = `${environment.apiUrl}/api/admin/assinantes`;

  constructor(private http: HttpClient) {}

  listarAssinantes(): Observable<AssinanteAdmin[]> {
    return this.http.get<AssinanteAdmin[]>(this.apiUrl);
  }

  obterMetricas(): Observable<AdminDashboardMetrics> {
    return this.http.get<AdminDashboardMetrics>(`${this.apiUrl}/metricas`);
  }

  atualizarStatus(empresaId: number, status: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${empresaId}/status`, { status });
  }
}

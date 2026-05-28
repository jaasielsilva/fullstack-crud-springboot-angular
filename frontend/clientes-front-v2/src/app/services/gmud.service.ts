import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { PageResponse } from '../models/page-response.model';
import {
  ChangeRequest,
  ChangeStatus,
  CreateChangeRequest,
  DeployEnvironment
} from '../models/gmud/change-request.model';

@Injectable({ providedIn: 'root' })
export class GmudService {
  private readonly apiUrl = `${environment.apiUrl}/api/gmud/changes`;

  constructor(private http: HttpClient) {}

  listar(
    status?: ChangeStatus,
    environment?: DeployEnvironment,
    page = 0,
    size = 10
  ): Observable<PageResponse<ChangeRequest>> {
    let params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));
    if (status) params = params.set('status', status);
    if (environment) params = params.set('environment', environment);
    return this.http.get<PageResponse<ChangeRequest>>(this.apiUrl, { params });
  }

  buscar(id: number): Observable<ChangeRequest> {
    return this.http.get<ChangeRequest>(`${this.apiUrl}/${id}`);
  }

  criar(dto: CreateChangeRequest): Observable<ChangeRequest> {
    return this.http.post<ChangeRequest>(this.apiUrl, dto);
  }

  submeter(id: number, comment?: string): Observable<ChangeRequest> {
    return this.http.post<ChangeRequest>(`${this.apiUrl}/${id}/submit`, { comment: comment ?? null });
  }

  aprovar(id: number, comment?: string): Observable<ChangeRequest> {
    return this.http.post<ChangeRequest>(`${this.apiUrl}/${id}/approve`, { comment: comment ?? null });
  }

  implantar(id: number, comment?: string): Observable<ChangeRequest> {
    return this.http.post<ChangeRequest>(`${this.apiUrl}/${id}/deploy`, { comment: comment ?? null });
  }

  rollback(id: number, comment: string): Observable<ChangeRequest> {
    return this.http.post<ChangeRequest>(`${this.apiUrl}/${id}/rollback`, { comment });
  }
}

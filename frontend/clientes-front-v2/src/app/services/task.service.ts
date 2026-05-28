import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { PageResponse } from '../models/page-response.model';
import { CreateWorkTask, TaskStatus, WorkTask } from '../models/task/work-task.model';

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly apiUrl = `${environment.apiUrl}/api/tasks`;

  constructor(private http: HttpClient) {}

  listar(status?: TaskStatus, page = 0, size = 10): Observable<PageResponse<WorkTask>> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));
    if (status) params = params.set('status', status);
    return this.http.get<PageResponse<WorkTask>>(this.apiUrl, { params });
  }

  buscar(id: number): Observable<WorkTask> {
    return this.http.get<WorkTask>(`${this.apiUrl}/${id}`);
  }

  criar(dto: CreateWorkTask): Observable<WorkTask> {
    return this.http.post<WorkTask>(this.apiUrl, dto);
  }

  iniciar(id: number): Observable<WorkTask> {
    return this.http.post<WorkTask>(`${this.apiUrl}/${id}/start`, {});
  }

  concluir(id: number): Observable<WorkTask> {
    return this.http.post<WorkTask>(`${this.apiUrl}/${id}/complete`, {});
  }

  cancelar(id: number): Observable<WorkTask> {
    return this.http.post<WorkTask>(`${this.apiUrl}/${id}/cancel`, {});
  }

  vincularGmud(id: number, changeId: number): Observable<WorkTask> {
    return this.http.post<WorkTask>(`${this.apiUrl}/${id}/link-gmud`, { changeId });
  }
}

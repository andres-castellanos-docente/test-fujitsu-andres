import { HttpClient, httpResource } from '@angular/common/http';
import { Injectable, InjectionToken, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AuditLog, EventRequest, EventResponse, EventType } from '../models/event.model';

/**
 * URL base da API. Usa caminho relativo: em desenvolvimento o proxy do Angular CLI
 * e, no Docker, o Nginx encaminham /api para o backend (sem necessidade de CORS).
 */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => '/api',
});

@Injectable({ providedIn: 'root' })
export class EventApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  create(request: EventRequest): Observable<EventResponse> {
    return this.http.post<EventResponse>(`${this.baseUrl}/events`, request);
  }

  /**
   * Recurso reativo da lista de eventos: refaz a requisição sempre que o filtro muda.
   * Deve ser chamado em contexto de injeção (ex.: inicialização de campo de componente).
   */
  eventsResource(type: () => EventType | null) {
    return httpResource<EventResponse[]>(
      () => {
        const selected = type();
        const params: Record<string, string> = selected ? { type: selected } : {};
        return { url: `${this.baseUrl}/events`, params };
      },
      { defaultValue: [] },
    );
  }

  auditsResource() {
    return httpResource<AuditLog[]>(() => `${this.baseUrl}/audits`, { defaultValue: [] });
  }
}

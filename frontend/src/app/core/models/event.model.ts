/** Tipos de evento aceitos pela API (espelha o enum EventType do backend). */
export const EVENT_TYPES = ['PRODUCTION', 'QUALITY'] as const;
export type EventType = (typeof EVENT_TYPES)[number];

export const EVENT_TYPE_LABELS: Record<EventType, string> = {
  PRODUCTION: 'Produção',
  QUALITY: 'Qualidade',
};

/** Payload enviado para POST /api/events. */
export interface EventRequest {
  eventId: string;
  eventType: EventType;
  description: string;
  /** ISO-8601 sem fuso, ex.: 2026-05-20T10:00:00 */
  timestamp: string;
}

/** Evento retornado pela API. */
export interface EventResponse extends EventRequest {
  /** Instante (UTC) em que a API recebeu o evento. */
  receivedAt: string;
}

export interface AuditLog {
  id: number;
  eventId: string;
  eventType: EventType;
  action: string;
  details: string;
  createdAt: string;
}

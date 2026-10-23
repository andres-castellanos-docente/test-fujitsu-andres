import { HttpErrorResponse } from '@angular/common/http';

/** Formato Problem Details (RFC 9457) devolvido pelo backend. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  correlationId?: string;
  errors?: Record<string, string>;
}

/**
 * Erro normalizado da API: componentes não precisam conhecer HttpErrorResponse
 * nem o formato bruto da resposta.
 */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
    readonly fieldErrors: Record<string, string> = {},
    readonly correlationId?: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }

  get isConflict(): boolean {
    return this.status === 409;
  }

  static fromHttpError(error: HttpErrorResponse): ApiError {
    const body = isProblemDetail(error.error) ? error.error : undefined;

    // 0 = sem resposta (CORS/rede); 502/503/504 sem Problem Details = proxy (Angular CLI/Nginx)
    // não conseguiu falar com o backend.
    if (error.status === 0 || (!body && GATEWAY_ERRORS.includes(error.status))) {
      return new ApiError(0, 'Não foi possível conectar à API. Verifique se o backend está em execução.');
    }

    const message = body?.detail ?? defaultMessage(error.status);
    return new ApiError(error.status, message, body?.errors ?? {}, body?.correlationId);
  }
}

const GATEWAY_ERRORS = [502, 503, 504];

function isProblemDetail(value: unknown): value is ProblemDetail {
  return typeof value === 'object' && value !== null && ('detail' in value || 'title' in value);
}

function defaultMessage(status: number): string {
  if (status >= 500) {
    return 'Erro interno no servidor. Tente novamente em instantes.';
  }
  if (status === 404) {
    return 'Recurso não encontrado.';
  }
  return 'Não foi possível concluir a operação.';
}

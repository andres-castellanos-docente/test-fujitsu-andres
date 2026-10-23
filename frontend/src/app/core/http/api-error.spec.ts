import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from './api-error';

describe('ApiError.fromHttpError', () => {
  it('usa o "detail" do Problem Details e expõe erros de campo', () => {
    const error = ApiError.fromHttpError(
      new HttpErrorResponse({
        status: 400,
        error: { title: 'Dados inválidos', detail: 'Um ou mais campos são inválidos', errors: { description: 'obrigatória' }, correlationId: 'abc' },
      }),
    );

    expect(error.status).toBe(400);
    expect(error.message).toBe('Um ou mais campos são inválidos');
    expect(error.fieldErrors).toEqual({ description: 'obrigatória' });
    expect(error.correlationId).toBe('abc');
    expect(error.isConflict).toBe(false);
  });

  it('identifica conflito (409) de evento duplicado', () => {
    const error = ApiError.fromHttpError(
      new HttpErrorResponse({ status: 409, error: { detail: "O evento 'EVT-1' já foi processado anteriormente" } }),
    );

    expect(error.isConflict).toBe(true);
    expect(error.message).toContain('EVT-1');
  });

  it('retorna mensagem amigável quando a API está inacessível', () => {
    const error = ApiError.fromHttpError(new HttpErrorResponse({ status: 0 }));

    expect(error.message).toContain('Não foi possível conectar à API');
  });

  it('trata 502/503/504 do proxy (backend fora do ar) como falha de conexão', () => {
    const error = ApiError.fromHttpError(new HttpErrorResponse({ status: 502, error: 'Bad Gateway' }));

    expect(error.message).toContain('Não foi possível conectar à API');
  });

  it('usa mensagem padrão para 500 sem Problem Details', () => {
    const error = ApiError.fromHttpError(new HttpErrorResponse({ status: 500, error: '<html>' }));

    expect(error.message).toContain('Erro interno');
  });
});

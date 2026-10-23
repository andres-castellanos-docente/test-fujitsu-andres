import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ApiError } from '../../../core/http/api-error';
import { EventApiService } from '../../../core/services/event-api.service';
import { NotificationService } from '../../../core/services/notification.service';
import { EventForm } from './event-form';
import { generateEventId, normalizeTimestamp, toLocalIsoString } from './event-form.utils';

describe('EventForm', () => {
  let fixture: ComponentFixture<EventForm>;
  let element: HTMLElement;
  const api = { create: vi.fn() };
  const notification = { success: vi.fn(), error: vi.fn() };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [EventForm],
      providers: [
        provideRouter([]),
        { provide: EventApiService, useValue: api },
        { provide: NotificationService, useValue: notification },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EventForm);
    element = fixture.nativeElement;
    await fixture.whenStable();
  });

  function type(selector: string, value: string): void {
    const input = element.querySelector<HTMLInputElement | HTMLTextAreaElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  async function submit(): Promise<void> {
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
  }

  it('não envia o formulário quando há campos obrigatórios vazios', async () => {
    await submit();

    expect(api.create).not.toHaveBeenCalled();
    expect(element.textContent).toContain('A descrição é obrigatória.');
  });

  it('envia o evento e exibe mensagem de sucesso', async () => {
    api.create.mockReturnValue(
      of({ eventId: 'EVT-1001', eventType: 'PRODUCTION', description: 'Linha iniciada', timestamp: '2026-05-20T10:00:00', receivedAt: '' }),
    );
    type('input[formcontrolname="eventId"]', 'EVT-1001');
    type('input[formcontrolname="timestamp"]', '2026-05-20T10:00');
    type('textarea[formcontrolname="description"]', '  Linha iniciada  ');

    await submit();

    expect(api.create).toHaveBeenCalledWith({
      eventId: 'EVT-1001',
      eventType: 'PRODUCTION',
      description: 'Linha iniciada',
      timestamp: '2026-05-20T10:00:00',
    });
    expect(notification.success).toHaveBeenCalledWith(expect.stringContaining('EVT-1001'));
    // formulário é limpo para o próximo cadastro
    expect(element.querySelector<HTMLTextAreaElement>('textarea')!.value).toBe('');
  });

  it('não exibe erros de validação após um cadastro bem-sucedido (estado "submitted" é limpo)', async () => {
    api.create.mockReturnValue(
      of({ eventId: 'EVT-1002', eventType: 'PRODUCTION', description: 'Ok', timestamp: '2026-05-20T10:00:00', receivedAt: '' }),
    );
    type('textarea[formcontrolname="description"]', 'Ok');

    await submit();

    expect(notification.success).toHaveBeenCalled();
    // feedback visual no botão (ícone de confirmação animado)
    expect(element.querySelector('button[type="submit"]')!.textContent).toContain('Registrado');
    expect(element.querySelectorAll('mat-error').length).toBe(0);
    expect(element.querySelector('.mat-form-field-invalid')).toBeNull();
  });

  it('exibe erro de duplicidade no campo e na notificação quando a API retorna 409', async () => {
    api.create.mockReturnValue(throwError(() => new ApiError(409, "O evento 'EVT-1001' já foi processado anteriormente")));
    type('input[formcontrolname="eventId"]', 'EVT-1001');
    type('textarea[formcontrolname="description"]', 'Duplicado');

    await submit();

    expect(notification.error).toHaveBeenCalledWith(expect.stringContaining('já foi processado'));
    expect(element.textContent).toContain('Este evento já foi processado.');
  });

  it('mostra erros de validação retornados pelo backend no campo correspondente', async () => {
    api.create.mockReturnValue(
      throwError(() => new ApiError(400, 'Um ou mais campos são inválidos', { description: 'Descrição rejeitada pelo servidor' })),
    );
    type('textarea[formcontrolname="description"]', 'x');

    await submit();

    expect(element.textContent).toContain('Descrição rejeitada pelo servidor');
  });
});

describe('event-form utils', () => {
  it('formata a data local no padrão ISO sem fuso', () => {
    expect(toLocalIsoString(new Date(2026, 4, 20, 9, 5, 7))).toBe('2026-05-20T09:05:07');
  });

  it('acrescenta segundos quando o input os omite', () => {
    expect(normalizeTimestamp('2026-05-20T10:00')).toBe('2026-05-20T10:00:00');
    expect(normalizeTimestamp('2026-05-20T10:00:30')).toBe('2026-05-20T10:00:30');
  });

  it('gera IDs no formato EVT-NNNNNN', () => {
    expect(generateEventId(() => 0)).toBe('EVT-100000');
    expect(generateEventId()).toMatch(/^EVT-\d{6}$/);
  });
});

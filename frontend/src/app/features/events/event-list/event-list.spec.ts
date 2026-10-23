import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { apiErrorInterceptor } from '../../../core/http/api-error.interceptor';
import { EventResponse } from '../../../core/models/event.model';
import { EventList, sortEvents } from './event-list';

const EVENTS: EventResponse[] = [
  { eventId: 'EVT-1', eventType: 'PRODUCTION', description: 'Linha iniciada', timestamp: '2026-05-20T10:00:00', receivedAt: '2026-05-20T13:00:00Z' },
  { eventId: 'EVT-2', eventType: 'QUALITY', description: 'Inspeção reprovada', timestamp: '2026-05-20T11:00:00', receivedAt: '2026-05-20T14:00:00Z' },
];

describe('EventList', () => {
  let fixture: ComponentFixture<EventList>;
  let httpMock: HttpTestingController;
  let element: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EventList],
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
        { provide: LOCALE_ID, useValue: 'en-US' },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(EventList);
    element = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('exibe os eventos retornados pela API em uma tabela', async () => {
    httpMock.expectOne('/api/events').flush(EVENTS);
    await fixture.whenStable();

    const rows = element.querySelectorAll('table tr.mat-mdc-row');
    expect(rows.length).toBe(2);
    // ordenação padrão: data/hora do evento decrescente
    expect(rows[0].textContent).toContain('EVT-2');
    expect(element.textContent).toContain('2 evento(s) exibido(s) · 1 de qualidade');
  });

  it('exibe mensagem de erro quando a API falha', async () => {
    httpMock.expectOne('/api/events').flush({ detail: 'Falha simulada' }, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Falha simulada');
  });
});

describe('sortEvents', () => {
  it('ordena de forma ascendente e descendente pela coluna escolhida', () => {
    expect(sortEvents(EVENTS, { active: 'eventId', direction: 'desc' }).map((e) => e.eventId)).toEqual(['EVT-2', 'EVT-1']);
    expect(sortEvents(EVENTS, { active: 'timestamp', direction: 'asc' }).map((e) => e.eventId)).toEqual(['EVT-1', 'EVT-2']);
  });

  it('mantém a ordem original sem direção definida', () => {
    expect(sortEvents(EVENTS, { active: 'eventId', direction: '' })).toEqual(EVENTS);
  });
});

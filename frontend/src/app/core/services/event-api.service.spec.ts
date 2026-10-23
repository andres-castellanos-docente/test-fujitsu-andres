import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Injector, runInInjectionContext, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { ApiError } from '../http/api-error';
import { apiErrorInterceptor } from '../http/api-error.interceptor';
import { EventRequest, EventType } from '../models/event.model';
import { EventApiService } from './event-api.service';

describe('EventApiService', () => {
  let service: EventApiService;
  let httpMock: HttpTestingController;

  const request: EventRequest = {
    eventId: 'EVT-1001',
    eventType: 'PRODUCTION',
    description: 'Production line started',
    timestamp: '2026-05-20T10:00:00',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([apiErrorInterceptor])), provideHttpClientTesting()],
    });
    service = TestBed.inject(EventApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('envia o evento via POST /api/events', async () => {
    const result = firstValueFrom(service.create(request));

    const req = httpMock.expectOne('/api/events');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ ...request, receivedAt: '2026-05-20T13:00:00Z' }, { status: 201, statusText: 'Created' });

    await expect(result).resolves.toMatchObject({ eventId: 'EVT-1001' });
  });

  it('converte o 409 da API em ApiError (via interceptor)', async () => {
    const result = firstValueFrom(service.create(request));

    httpMock
      .expectOne('/api/events')
      .flush({ status: 409, detail: "O evento 'EVT-1001' já foi processado anteriormente" }, { status: 409, statusText: 'Conflict' });

    const error = await result.catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).isConflict).toBe(true);
  });

  it('refaz a consulta com o filtro de tipo quando ele muda', () => {
    const type = signal<EventType | null>(null);
    const resource = runInInjectionContext(TestBed.inject(Injector), () => service.eventsResource(type));

    TestBed.tick();
    httpMock.expectOne((r) => r.url === '/api/events' && !r.params.has('type')).flush([]);

    type.set('QUALITY');
    TestBed.tick();
    httpMock.expectOne((r) => r.url === '/api/events' && r.params.get('type') === 'QUALITY').flush([]);

    expect(resource.value()).toEqual([]);
  });
});

import { Routes } from '@angular/router';

/** Rotas com lazy loading: cada tela é carregada apenas quando acessada. */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'eventos/novo' },
  {
    path: 'eventos/novo',
    title: 'Cadastrar evento | Event Processor',
    loadComponent: () => import('./features/events/event-form/event-form').then((m) => m.EventForm),
  },
  {
    path: 'eventos',
    title: 'Eventos registrados | Event Processor',
    loadComponent: () => import('./features/events/event-list/event-list').then((m) => m.EventList),
  },
  {
    path: 'auditoria',
    title: 'Auditoria | Event Processor',
    loadComponent: () => import('./features/audits/audit-list/audit-list').then((m) => m.AuditList),
  },
  { path: '**', redirectTo: 'eventos/novo' },
];

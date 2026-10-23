import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App (shell)', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('renderiza a navegação principal e o botão de tema', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    const links = Array.from(element.querySelectorAll('mat-nav-list a')).map((a) => a.textContent?.trim());
    expect(links).toEqual(['add_circleCadastrar evento', 'table_viewEventos registrados', 'verified_userAuditoria']);
    expect(element.querySelector('app-theme-toggle button')?.getAttribute('aria-label')).toMatch(/Ativar modo/);
  });
});

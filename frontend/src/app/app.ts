import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatListItem, MatListItemIcon, MatListItemTitle, MatNavList } from '@angular/material/list';
import { MatSidenav, MatSidenavContainer, MatSidenavContent } from '@angular/material/sidenav';
import { MatToolbar } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';
import { ThemeToggle } from './shared/theme-toggle/theme-toggle';

interface NavItem {
  path: string;
  label: string;
  icon: string;
}

/**
 * Shell da aplicação: toolbar + menu lateral responsivo.
 * Desktop: menu fixo ao lado. Mobile/tablet: menu sobreposto, aberto pelo botão de menu.
 */
@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbar,
    MatIconButton,
    MatIcon,
    MatSidenavContainer,
    MatSidenav,
    MatSidenavContent,
    MatNavList,
    MatListItem,
    MatListItemIcon,
    MatListItemTitle,
    ThemeToggle,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly navItems: NavItem[] = [
    { path: '/eventos/novo', label: 'Cadastrar evento', icon: 'add_circle' },
    { path: '/eventos', label: 'Eventos registrados', icon: 'table_view' },
    { path: '/auditoria', label: 'Auditoria', icon: 'verified_user' },
  ];

  protected readonly isHandset = toSignal(
    inject(BreakpointObserver)
      .observe([Breakpoints.XSmall, Breakpoints.Small])
      .pipe(map((result) => result.matches)),
    { initialValue: false },
  );

  /**
   * Liga a cascata de entrada dos itens do menu. Começa `true` (anima no carregamento)
   * e é religada a cada abertura do menu no mobile, reiniciando a animação CSS.
   */
  protected readonly menuAnimating = signal(true);

  protected closeOnHandset(sidenav: MatSidenav): void {
    if (this.isHandset()) {
      void sidenav.close();
    }
  }
}

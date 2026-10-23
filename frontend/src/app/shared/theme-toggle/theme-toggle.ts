import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';
import { ThemeService } from '../../core/services/theme.service';

@Component({
  selector: 'app-theme-toggle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIconButton, MatIcon, MatTooltip],
  template: `
    <button
      mat-icon-button
      type="button"
      (click)="theme.toggle()"
      [matTooltip]="label()"
      [attr.aria-label]="label()"
      [attr.aria-pressed]="theme.isDark()"
    >
      <!-- Cada troca recria o ícone, que entra girando (animate.enter nativo) -->
      @if (theme.isDark()) {
        <mat-icon aria-hidden="true" animate.enter="icon-swap-in">light_mode</mat-icon>
      } @else {
        <mat-icon aria-hidden="true" animate.enter="icon-swap-in">dark_mode</mat-icon>
      }
    </button>
  `,
})
export class ThemeToggle {
  protected readonly theme = inject(ThemeService);

  protected label(): string {
    return this.theme.isDark() ? 'Ativar modo claro' : 'Ativar modo escuro';
  }
}

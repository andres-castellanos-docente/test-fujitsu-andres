import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

/**
 * Mensagens de feedback (sucesso/erro). O MatSnackBar anuncia o texto para
 * leitores de tela via LiveAnnouncer.
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.snackBar.open(message, 'OK', { duration: 4000, panelClass: 'snack-success', politeness: 'polite' });
  }

  error(message: string): void {
    this.snackBar.open(message, 'Fechar', { duration: 7000, panelClass: 'snack-error', politeness: 'assertive' });
  }
}

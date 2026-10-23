import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormGroupDirective, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatError, MatFormField, MatHint, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatOption, MatSelect } from '@angular/material/select';
import { MatTooltip } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../../core/http/api-error';
import { EVENT_TYPE_LABELS, EVENT_TYPES, EventRequest, EventType } from '../../../core/models/event.model';
import { EventApiService } from '../../../core/services/event-api.service';
import { NotificationService } from '../../../core/services/notification.service';
import { EventTypeBadge } from '../../../shared/event-type-badge/event-type-badge';
import { PageHeader } from '../../../shared/page-header/page-header';
import { generateEventId, normalizeTimestamp, toLocalIsoString } from './event-form.utils';

export const DESCRIPTION_MAX_LENGTH = 500;

@Component({
  selector: 'app-event-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCard,
    MatCardContent,
    MatFormField,
    MatLabel,
    MatHint,
    MatError,
    MatSuffix,
    MatInput,
    MatSelect,
    MatOption,
    MatButton,
    MatIconButton,
    MatIcon,
    MatTooltip,
    MatProgressSpinner,
    EventTypeBadge,
    PageHeader,
  ],
  templateUrl: './event-form.html',
})
export class EventForm {
  private readonly api = inject(EventApiService);
  private readonly notification = inject(NotificationService);

  protected readonly eventTypes = EVENT_TYPES;
  protected readonly typeLabels = EVENT_TYPE_LABELS;
  protected readonly descriptionMaxLength = DESCRIPTION_MAX_LENGTH;
  protected readonly submitting = signal(false);

  /**
   * Diretiva [formGroup] do template. Necessária para limpar o estado "submitted":
   * o ErrorStateMatcher do Material exibe erros quando o formulário foi submetido,
   * e `FormGroup.reset()` sozinho não zera esse estado.
   */
  private readonly formDirective = viewChild.required(FormGroupDirective);
  private readonly formCard = viewChild.required('formCard', { read: ElementRef<HTMLElement> });

  /** Mostra o ícone de confirmação no botão por alguns instantes após salvar. */
  protected readonly justSaved = signal(false);
  private justSavedTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.justSavedTimer));
  }

  protected readonly form = inject(NonNullableFormBuilder).group({
    eventId: [generateEventId(), [Validators.required, Validators.maxLength(50), Validators.pattern(/^[A-Za-z0-9_-]+$/)]],
    eventType: ['PRODUCTION' as EventType, Validators.required],
    description: ['', [Validators.required, Validators.maxLength(DESCRIPTION_MAX_LENGTH)]],
    timestamp: [toLocalIsoString(new Date()), Validators.required],
  });

  protected regenerateId(): void {
    this.form.controls.eventId.setValue(generateEventId());
  }

  protected submit(): void {
    if (this.submitting()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.shakeCard();
      return;
    }

    const raw = this.form.getRawValue();
    const request: EventRequest = {
      eventId: raw.eventId.trim(),
      eventType: raw.eventType,
      description: raw.description.trim(),
      timestamp: normalizeTimestamp(raw.timestamp),
    };

    this.submitting.set(true);
    this.api.create(request).subscribe({
      next: (created) => {
        this.submitting.set(false);
        const audit = created.eventType === 'QUALITY' ? ' Registro de auditoria gerado.' : '';
        this.notification.success(`Evento ${created.eventId} registrado com sucesso.${audit}`);
        this.resetForm(created.eventType);
        this.flashSaved();
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.handleError(error);
      },
    });
  }

  private handleError(error: unknown): void {
    if (!(error instanceof ApiError)) {
      this.notification.error('Erro inesperado ao enviar o evento.');
      return;
    }

    if (error.isConflict) {
      this.form.controls.eventId.setErrors({ duplicate: true });
      this.form.controls.eventId.markAsTouched();
    }

    // Erros de validação do backend aparecem no próprio campo
    for (const [field, message] of Object.entries(error.fieldErrors)) {
      const control = this.form.get(field);
      control?.setErrors({ server: message });
      control?.markAsTouched();
    }

    this.notification.error(error.message);
    this.shakeCard();
  }

  private flashSaved(): void {
    clearTimeout(this.justSavedTimer);
    this.justSaved.set(true);
    this.justSavedTimer = setTimeout(() => this.justSaved.set(false), 1600);
  }

  /**
   * Balança o card para sinalizar que o envio não foi aceito. Usa a Web Animations API
   * porque, ao contrário de uma classe CSS, pode ser reiniciada a cada tentativa.
   */
  private shakeCard(): void {
    const element = this.formCard().nativeElement;
    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    if (reduceMotion || typeof element.animate !== 'function') {
      return;
    }
    element.animate(
      [
        { transform: 'translateX(0)' },
        { transform: 'translateX(-8px)' },
        { transform: 'translateX(8px)' },
        { transform: 'translateX(-6px)' },
        { transform: 'translateX(6px)' },
        { transform: 'translateX(0)' },
      ],
      { duration: 400, easing: 'ease-in-out' },
    );
  }

  protected resetForm(lastType: EventType = 'PRODUCTION'): void {
    // resetForm() = form.reset(valor) + submitted = false (remove os mat-error)
    this.formDirective().resetForm({
      eventId: generateEventId(),
      eventType: lastType,
      description: '',
      timestamp: toLocalIsoString(new Date()),
    });
  }
}

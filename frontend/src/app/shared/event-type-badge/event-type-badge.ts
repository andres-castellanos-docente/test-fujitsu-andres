import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { EVENT_TYPE_LABELS, EventType } from '../../core/models/event.model';

/** Selo visual do tipo de evento (cor + ícone + texto, sem depender só da cor). */
@Component({
  selector: 'app-event-type-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span
      class="inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold tracking-wide"
      [class]="colorClasses()"
    >
      <span aria-hidden="true">{{ type() === 'QUALITY' ? '◆' : '●' }}</span>
      {{ label() }}
    </span>
  `,
})
export class EventTypeBadge {
  readonly type = input.required<EventType>();

  protected readonly label = computed(() => EVENT_TYPE_LABELS[this.type()]);

  protected readonly colorClasses = computed(() =>
    this.type() === 'QUALITY'
      ? 'bg-amber-100 text-amber-900 dark:bg-amber-900/40 dark:text-amber-100'
      : 'bg-sky-100 text-sky-900 dark:bg-sky-900/40 dark:text-sky-100',
  );
}

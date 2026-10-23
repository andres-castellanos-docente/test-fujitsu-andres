import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatButtonToggle, MatButtonToggleGroup } from '@angular/material/button-toggle';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatFormField, MatLabel, MatPrefix } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatProgressBar } from '@angular/material/progress-bar';
import { MatSort, MatSortHeader, Sort } from '@angular/material/sort';
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell,
  MatHeaderCellDef,
  MatHeaderRow,
  MatHeaderRowDef,
  MatNoDataRow,
  MatRow,
  MatRowDef,
  MatTable,
} from '@angular/material/table';
import { MatTooltip } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../../core/http/api-error';
import { EventResponse, EventType } from '../../../core/models/event.model';
import { EventApiService } from '../../../core/services/event-api.service';
import { EventTypeBadge } from '../../../shared/event-type-badge/event-type-badge';
import { PageHeader } from '../../../shared/page-header/page-header';

type SortableColumn = 'eventId' | 'eventType' | 'description' | 'timestamp' | 'receivedAt';

@Component({
  selector: 'app-event-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    RouterLink,
    MatTable,
    MatColumnDef,
    MatHeaderCell,
    MatHeaderCellDef,
    MatCell,
    MatCellDef,
    MatHeaderRow,
    MatHeaderRowDef,
    MatRow,
    MatRowDef,
    MatNoDataRow,
    MatSort,
    MatSortHeader,
    MatButtonToggleGroup,
    MatButtonToggle,
    MatFormField,
    MatLabel,
    MatPrefix,
    MatInput,
    MatButton,
    MatIconButton,
    MatIcon,
    MatTooltip,
    MatProgressBar,
    MatCard,
    MatCardContent,
    EventTypeBadge,
    PageHeader,
  ],
  templateUrl: './event-list.html',
  styleUrl: './event-list.scss',
})
export class EventList {
  protected readonly displayedColumns: SortableColumn[] = ['eventId', 'eventType', 'description', 'timestamp', 'receivedAt'];

  /** Filtro por tipo: aplicado no backend (GET /api/events?type=). */
  protected readonly typeFilter = signal<EventType | null>(null);
  /** Busca textual: aplicada localmente sobre os dados já carregados. */
  protected readonly search = signal('');
  protected readonly sort = signal<Sort>({ active: 'timestamp', direction: 'desc' });

  protected readonly events = inject(EventApiService).eventsResource(this.typeFilter);

  protected readonly errorMessage = computed(() => {
    const error = this.events.error();
    if (!error) {
      return null;
    }
    return error instanceof ApiError ? error.message : 'Não foi possível carregar os eventos.';
  });

  protected readonly rows = computed(() => {
    const data = this.events.hasValue() ? this.events.value() : [];
    const term = this.search().trim().toLowerCase();
    const filtered = term
      ? data.filter((e) => e.eventId.toLowerCase().includes(term) || e.description.toLowerCase().includes(term))
      : data;
    return sortEvents(filtered, this.sort());
  });

  protected readonly totals = computed(() => {
    const rows = this.rows();
    return {
      all: rows.length,
      quality: rows.filter((e) => e.eventType === 'QUALITY').length,
    };
  });

  protected onSearch(event: Event): void {
    this.search.set((event.target as HTMLInputElement).value);
  }
}

export function sortEvents(events: EventResponse[], sort: Sort): EventResponse[] {
  const column = sort.active as SortableColumn;
  if (!sort.direction) {
    return events;
  }
  const factor = sort.direction === 'asc' ? 1 : -1;
  return [...events].sort((a, b) => a[column].localeCompare(b[column]) * factor);
}

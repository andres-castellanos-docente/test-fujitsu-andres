import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatProgressBar } from '@angular/material/progress-bar';
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
import { ApiError } from '../../../core/http/api-error';
import { EventApiService } from '../../../core/services/event-api.service';
import { EventTypeBadge } from '../../../shared/event-type-badge/event-type-badge';
import { PageHeader } from '../../../shared/page-header/page-header';

/** Consulta dos registros de auditoria gerados por eventos QUALITY. */
@Component({
  selector: 'app-audit-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
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
  templateUrl: './audit-list.html',
})
export class AuditList {
  protected readonly displayedColumns = ['createdAt', 'eventId', 'eventType', 'action', 'details'];
  protected readonly audits = inject(EventApiService).auditsResource();

  protected readonly rows = computed(() => (this.audits.hasValue() ? this.audits.value() : []));

  protected readonly errorMessage = computed(() => {
    const error = this.audits.error();
    if (!error) {
      return null;
    }
    return error instanceof ApiError ? error.message : 'Não foi possível carregar a auditoria.';
  });
}

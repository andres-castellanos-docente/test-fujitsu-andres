import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Cabeçalho padrão das telas: título, subtítulo e área de ações (ng-content). */
@Component({
  selector: 'app-page-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 class="m-0 text-2xl font-medium text-on-surface sm:text-3xl">{{ title() }}</h1>
        @if (subtitle()) {
          <p class="mt-1 mb-0 text-sm text-on-surface-variant">{{ subtitle() }}</p>
        }
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <ng-content />
      </div>
    </header>
  `,
})
export class PageHeader {
  readonly title = input.required<string>();
  readonly subtitle = input<string>();
}

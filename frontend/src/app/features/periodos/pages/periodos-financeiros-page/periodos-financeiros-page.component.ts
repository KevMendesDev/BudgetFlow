import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { PageSize } from '../../../../core/models/pagination.models';
import { PeriodoFinanceiroResponse } from '../../../../core/models/periodo-financeiro.models';
import { PeriodosFinanceirosApiService } from '../../../../core/services/periodos-financeiros-api.service';
import { ToastService } from '../../../../core/services/toast.service';
import { ConfirmDialogService } from '../../../../shared/services/confirm-dialog.service';
import { DateBRPipe } from '../../../../shared/pipes/date-br.pipe';
import { mapApiError } from '../../../../shared/utils/error-message.util';
import { formatMonthYear } from '../../../../shared/utils/format.util';
import { isDesktopViewport } from '../../../../shared/utils/viewport.util';
import { PeriodoFinanceiroModalComponent } from '../../components/periodo-financeiro-modal/periodo-financeiro-modal.component';

@Component({
  selector: 'app-periodos-financeiros-page',
  imports: [ReactiveFormsModule, DateBRPipe, PeriodoFinanceiroModalComponent],
  templateUrl: './periodos-financeiros-page.component.html',
  styleUrl: './periodos-financeiros-page.component.scss',
})
export class PeriodosFinanceirosPageComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly periodosApi = inject(PeriodosFinanceirosApiService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly destroyRef = inject(DestroyRef);

  readonly periodos = signal<PeriodoFinanceiroResponse[]>([]);
  readonly loading = signal(false);
  readonly deletingId = signal<number | null>(null);
  readonly modalOpen = signal(false);
  readonly editingPeriodo = signal<PeriodoFinanceiroResponse | null>(null);
  readonly errorMessage = signal('');
  readonly filtrosAbertos = signal(isDesktopViewport());
  readonly paginaAtual = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly formatMonthYear = formatMonthYear;

  readonly filtersForm = this.formBuilder.nonNullable.group({
    q: [''],
    dataInicio: [''],
    dataFim: [''],
  });

  ngOnInit(): void {
    this.loadPeriodos(0);

    this.filtersForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => this.loadPeriodos(0));
  }

  toggleFiltros(): void {
    this.filtrosAbertos.update((v) => !v);
  }

  applyFilters(): void {
    this.loadPeriodos(0);
  }

  clearFilters(): void {
    this.filtersForm.setValue({ q: '', dataInicio: '', dataFim: '' }, { emitEvent: false });
    this.loadPeriodos(0);
  }

  openCreateModal(): void {
    this.editingPeriodo.set(null);
    this.modalOpen.set(true);
  }

  startEdit(periodo: PeriodoFinanceiroResponse): void {
    this.editingPeriodo.set(periodo);
    this.modalOpen.set(true);
  }

  closeModal(): void {
    this.modalOpen.set(false);
    this.editingPeriodo.set(null);
  }

  onPeriodoSaved(): void {
    this.closeModal();
    this.loadPeriodos(this.paginaAtual());
  }

  async deletePeriodo(periodo: PeriodoFinanceiroResponse): Promise<void> {
    if (this.deletingId()) {
      return;
    }

    const confirmed = await this.confirmDialog.confirm(
      `Excluir o período de ${periodo.dataInicio} até ${periodo.dataFim}?`
    );
    if (!confirmed) {
      return;
    }

    this.deletingId.set(periodo.id);

    this.periodosApi
      .delete(periodo.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.deletingId.set(null);
          this.toast.show('Período excluído.', 'success');
          this.loadPeriodos(this.paginaAtual());
        },
        error: () => {
          this.deletingId.set(null);
        },
      });
  }

  goToPreviousPage(): void {
    const atual = this.paginaAtual();
    if (atual > 0) {
      this.loadPeriodos(atual - 1);
    }
  }

  goToNextPage(): void {
    const atual = this.paginaAtual();
    if (atual + 1 < this.totalPages()) {
      this.loadPeriodos(atual + 1);
    }
  }

  private loadPeriodos(page = this.paginaAtual()): void {
    this.loading.set(true);
    this.errorMessage.set('');

    const filters = this.filtersForm.getRawValue();

    this.periodosApi
      .listAll({
        page,
        size: PageSize.DEFAULT,
        q: filters.q,
        dataInicio: filters.dataInicio,
        dataFim: filters.dataFim,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          const totalPages = page.page.totalPages ?? page.totalPages ?? 0;
          const totalElements = page.page.totalElements ?? page.totalElements ?? 0;

          if (page.content.length === 0 && totalPages > 0 && this.paginaAtual() >= totalPages) {
            this.loadPeriodos(totalPages - 1);
            return;
          }

          this.periodos.set(page.content);
          this.paginaAtual.set(page.page.number ?? page.number ?? 0);
          this.totalPages.set(totalPages);
          this.totalElements.set(totalElements);
          this.loading.set(false);
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
          this.totalPages.set(0);
          this.totalElements.set(0);
          this.loading.set(false);
        },
      });
  }
}

import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { PageSize } from '../../../../core/models/pagination.models';
import { PeriodoFinanceiroResponse } from '../../../../core/models/periodo-financeiro.models';
import { PeriodosFinanceirosApiService } from '../../../../core/services/periodos-financeiros-api.service';
import { ToastService } from '../../../../core/services/toast.service';
import { ConfirmDialogService } from '../../../../shared/services/confirm-dialog.service';
import { DateBRPipe } from '../../../../shared/pipes/date-br.pipe';
import { mapApiError } from '../../../../shared/utils/error-message.util';
import { fieldError } from '../../../../shared/utils/form-error.util';
import { isDesktopViewport } from '../../../../shared/utils/viewport.util';

@Component({
  selector: 'app-periodos-financeiros-page',
  imports: [ReactiveFormsModule, DateBRPipe],
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
  readonly submitting = signal(false);
  readonly deletingId = signal<number | null>(null);
  readonly modalOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly errorMessage = signal('');
  readonly modalErrorMessage = signal('');
  readonly filtrosAbertos = signal(isDesktopViewport());
  readonly paginaAtual = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly fieldError = fieldError;

  readonly filtersForm = this.formBuilder.nonNullable.group({
    q: [''],
    dataInicio: [''],
    dataFim: [''],
  });

  readonly form = this.formBuilder.nonNullable.group({
    dataInicio: ['', [Validators.required]],
    dataFim: ['', [Validators.required]],
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
    this.modalOpen.set(true);
    this.resetForm();
  }

  startEdit(periodo: PeriodoFinanceiroResponse): void {
    this.modalOpen.set(true);
    this.editingId.set(periodo.id);
    this.form.setValue({
      dataInicio: periodo.dataInicio,
      dataFim: periodo.dataFim,
    });
    this.modalErrorMessage.set('');
  }

  closeModal(): void {
    this.modalOpen.set(false);
    this.resetForm();
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    if (raw.dataFim < raw.dataInicio) {
      this.modalErrorMessage.set('A data de fim não pode ser anterior à data de início.');
      return;
    }

    this.modalErrorMessage.set('');
    this.submitting.set(true);

    const payload = { dataInicio: raw.dataInicio, dataFim: raw.dataFim };

    const editingId = this.editingId();
    const request$ = editingId
      ? this.periodosApi.update(editingId, payload)
      : this.periodosApi.create(payload);

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.show(editingId ? 'Período atualizado.' : 'Período criado.', 'success');
        this.closeModal();
        this.loadPeriodos(this.paginaAtual());
      },
      error: (err) => {
        this.modalErrorMessage.set(mapApiError(err));
        this.submitting.set(false);
      },
    });
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

  private resetForm(): void {
    this.editingId.set(null);
    this.form.setValue({ dataInicio: '', dataFim: '' });
    this.form.markAsPristine();
    this.form.markAsUntouched();
    this.modalErrorMessage.set('');
  }
}

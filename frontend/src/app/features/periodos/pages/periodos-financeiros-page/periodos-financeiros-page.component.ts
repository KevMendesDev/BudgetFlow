import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';

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
    this.loadPeriodos();

    this.filtersForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => this.loadPeriodos());
  }

  toggleFiltros(): void {
    this.filtrosAbertos.update((v) => !v);
  }

  applyFilters(): void {
    this.loadPeriodos();
  }

  clearFilters(): void {
    this.filtersForm.setValue({ q: '', dataInicio: '', dataFim: '' }, { emitEvent: false });
    this.loadPeriodos();
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
        this.loadPeriodos();
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
          this.loadPeriodos();
        },
        error: () => {
          this.deletingId.set(null);
        },
      });
  }

  private loadPeriodos(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    const filters = this.filtersForm.getRawValue();

    this.periodosApi
      .listAll({
        q: filters.q,
        dataInicio: filters.dataInicio,
        dataFim: filters.dataFim,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.periodos.set(page.content);
          this.loading.set(false);
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
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

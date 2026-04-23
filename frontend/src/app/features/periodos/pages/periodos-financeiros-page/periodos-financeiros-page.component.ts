import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { PeriodoFinanceiroResponse } from '../../../../core/models/periodo-financeiro.models';
import { PeriodosFinanceirosApiService } from '../../../../core/services/periodos-financeiros-api.service';
import { ToastService } from '../../../../core/services/toast.service';
import { mapApiError } from '../../../../shared/utils/error-message.util';

@Component({
  selector: 'app-periodos-financeiros-page',
  imports: [ReactiveFormsModule],
  templateUrl: './periodos-financeiros-page.component.html',
  styleUrl: './periodos-financeiros-page.component.scss',
})
export class PeriodosFinanceirosPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly periodosApi = inject(PeriodosFinanceirosApiService);
  private readonly toast = inject(ToastService);

  readonly periodos = signal<PeriodoFinanceiroResponse[]>([]);
  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly deletingId = signal<number | null>(null);
  readonly modalOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly errorMessage = signal('');
  readonly modalErrorMessage = signal('');

  readonly filtersForm = this.formBuilder.nonNullable.group({
    q: [''],
    dataInicio: [''],
    dataFim: [''],
  });

  readonly form = this.formBuilder.nonNullable.group({
    dataInicio: ['', [Validators.required]],
    dataFim: ['', [Validators.required]],
  });

  constructor() {
    this.loadPeriodos();
  }

  applyFilters(): void {
    this.loadPeriodos();
  }

  clearFilters(): void {
    this.filtersForm.setValue({ q: '', dataInicio: '', dataFim: '' });
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

  fieldError(control: AbstractControl | null, label: string): string {
    if (!control || !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return `${label} obrigatoria`;
    }

    return '';
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    if (raw.dataFim < raw.dataInicio) {
      this.modalErrorMessage.set('A data de fim nao pode ser anterior a data de inicio.');
      return;
    }

    this.modalErrorMessage.set('');
    this.submitting.set(true);

    const payload = {
      dataInicio: raw.dataInicio,
      dataFim: raw.dataFim,
    };

    const editingId = this.editingId();
    const request$ = editingId
      ? this.periodosApi.update(editingId, payload)
      : this.periodosApi.create(payload);

    request$.subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.show(editingId ? 'Periodo atualizado.' : 'Periodo criado.', 'success');
        this.closeModal();
        this.loadPeriodos();
      },
      error: (err) => {
        this.modalErrorMessage.set(mapApiError(err));
        this.submitting.set(false);
      },
    });
  }

  deletePeriodo(periodo: PeriodoFinanceiroResponse): void {
    if (this.deletingId()) {
      return;
    }

    const confirmDelete = window.confirm(
      `Excluir periodo de ${this.formatDate(periodo.dataInicio)} ate ${this.formatDate(periodo.dataFim)}?`
    );

    if (!confirmDelete) {
      return;
    }

    this.deletingId.set(periodo.id);

    this.periodosApi.delete(periodo.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.toast.show('Periodo excluido.', 'success');
        this.loadPeriodos();
      },
      error: (err) => {
        this.deletingId.set(null);
        this.toast.show(mapApiError(err), 'error');
      },
    });
  }

  formatDate(value: string): string {
    return new Date(`${value}T00:00:00`).toLocaleDateString('pt-BR');
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

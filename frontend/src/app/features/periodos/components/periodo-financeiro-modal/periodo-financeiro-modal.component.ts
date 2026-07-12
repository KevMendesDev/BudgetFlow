import { Component, DestroyRef, inject, input, OnInit, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { PeriodoFinanceiroResponse } from '../../../../core/models/periodo-financeiro.models';
import { PeriodosFinanceirosApiService } from '../../../../core/services/periodos-financeiros-api.service';
import { ToastService } from '../../../../core/services/toast.service';
import { mapApiError } from '../../../../shared/utils/error-message.util';
import { fieldError } from '../../../../shared/utils/form-error.util';
import { MONTH_OPTIONS } from '../../../../shared/utils/format.util';

@Component({
  selector: 'app-periodo-financeiro-modal',
  imports: [ReactiveFormsModule],
  templateUrl: './periodo-financeiro-modal.component.html',
})
export class PeriodoFinanceiroModalComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly periodosApi = inject(PeriodosFinanceirosApiService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly editingPeriodo = input<PeriodoFinanceiroResponse | null>(null);

  readonly saved = output<PeriodoFinanceiroResponse>();
  readonly closed = output<void>();

  readonly fieldError = fieldError;
  readonly monthOptions = MONTH_OPTIONS;

  readonly submitting = signal(false);
  readonly errorMessage = signal('');

  readonly form = this.formBuilder.nonNullable.group({
    mes: ['', [Validators.required]],
    ano: ['', [Validators.required, Validators.min(2000), Validators.max(2100)]],
  });

  ngOnInit(): void {
    const periodo = this.editingPeriodo();
    if (periodo) {
      this.form.setValue({
        mes: String(periodo.mes),
        ano: String(periodo.ano),
      });
      return;
    }

    this.form.controls.ano.setValue(String(new Date().getFullYear()));
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set('');
    this.submitting.set(true);

    const raw = this.form.getRawValue();
    const payload = { mes: Number(raw.mes), ano: Number(raw.ano) };

    const editingId = this.editingPeriodo()?.id;
    const request$ = editingId
      ? this.periodosApi.update(editingId, payload)
      : this.periodosApi.create(payload);

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (periodo) => {
        this.submitting.set(false);
        this.toast.show(editingId ? 'Período atualizado.' : 'Período criado.', 'success');
        this.saved.emit(periodo);
      },
      error: (err) => {
        this.errorMessage.set(mapApiError(err));
        this.submitting.set(false);
      },
    });
  }

  close(): void {
    this.closed.emit();
  }
}

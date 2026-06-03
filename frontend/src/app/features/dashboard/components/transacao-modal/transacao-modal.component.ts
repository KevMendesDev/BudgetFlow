import { Component, computed, DestroyRef, inject, input, OnInit, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CategoriaResponse, CLASSIFICACAO_LABELS } from '../../../../core/models/categoria.models';
import { PeriodoFinanceiro } from '../../../../core/models/periodo-financeiro.models';
import { TransacaoRecorrenteResponse } from '../../../../core/models/transacao-recorrente.models';
import { TransacoesApiService } from '../../../../core/services/transacoes-api.service';
import { ToastService } from '../../../../core/services/toast.service';
import {
  TIPOS_MOVIMENTACAO,
  TIPOS_PAGAMENTO,
  TipoMovimentacao,
  TipoPagamento,
  TransacaoResponse,
} from '../../../../core/models/transacao.models';
import { fieldError } from '../../../../shared/utils/form-error.util';
import { formatDate, toIsoDate } from '../../../../shared/utils/format.util';
import { mapApiError } from '../../../../shared/utils/error-message.util';
@Component({
  selector: 'app-transacao-modal',
  imports: [ReactiveFormsModule],
  templateUrl: './transacao-modal.component.html',
  styleUrl: './transacao-modal.component.scss',
})
export class TransacaoModalComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly transacoesApi = inject(TransacoesApiService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly categorias = input.required<CategoriaResponse[]>();
  readonly recorrentes = input.required<TransacaoRecorrenteResponse[]>();
  readonly transacoes = input.required<TransacaoResponse[]>();
  readonly selectedPeriodo = input<PeriodoFinanceiro | null>(null);
  readonly editingTransacao = input<TransacaoResponse | null>(null);

  readonly saved = output<void>();
  readonly closed = output<void>();

  readonly tiposMovimentacao = TIPOS_MOVIMENTACAO;
  readonly tiposPagamento = TIPOS_PAGAMENTO;
  readonly classificacaoLabels = CLASSIFICACAO_LABELS;
  readonly fieldError = fieldError;

  readonly submitting = signal(false);
  readonly errorMessage = signal('');
  readonly editingId = computed(() => this.editingTransacao()?.id ?? null);

  readonly form = this.formBuilder.nonNullable.group({
    transacaoRecorrenteId: [''],
    categoriaId: [''],
    descricao: ['', [Validators.maxLength(255)]],
    valor: [''],
    tipoMovimentacao: ['' as '' | TipoMovimentacao],
    tipoPagamento: ['' as '' | TipoPagamento],
    data: ['', [Validators.required]],
  });

  readonly recorrentesDisponiveis = computed(() => {
    const selectedRecorrenteId = Number(this.form.controls.transacaoRecorrenteId.value || 0);
    const idsUsados = new Set(
      this.transacoes()
        .filter((tx) => tx.transacaoRecorrenteId)
        .map((tx) => tx.transacaoRecorrenteId as number)
    );

    return this.recorrentes().filter((recorrente) => {
      if (selectedRecorrenteId === recorrente.id) {
        return true;
      }
      return !idsUsados.has(recorrente.id);
    });
  });

  ngOnInit(): void {
    const tx = this.editingTransacao();
    if (tx) {
      this.populateForm(tx);
    } else {
      this.setInitialDate();
    }

    this.form.controls.transacaoRecorrenteId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => {
        this.toggleManualValidators(!value);
      });
  }

  isManualMode(): boolean {
    return !this.form.controls.transacaoRecorrenteId.value;
  }

  onRecorrenteChange(rawId: string): void {
    const recorrenteId = Number(rawId);
    if (!Number.isFinite(recorrenteId) || recorrenteId < 1) {
      return;
    }

    const recorrente = this.recorrentes().find((item) => item.id === recorrenteId);
    if (!recorrente) {
      return;
    }

    const periodo = this.selectedPeriodo();
    const dataPadrao = periodo
      ? this.clampDateToPeriodo(recorrente.dataInicio, periodo)
      : recorrente.dataInicio;

    this.form.patchValue({
      categoriaId: String(recorrente.categoriaId),
      descricao: recorrente.descricao,
      valor: String(recorrente.valorParcela),
      tipoMovimentacao: recorrente.tipoMovimentacao,
      tipoPagamento: recorrente.tipoPagamento,
      data: dataPadrao,
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const periodo = this.selectedPeriodo();
    if (!periodo) {
      this.errorMessage.set('Selecione um período válido para lançar a transação.');
      return;
    }

    this.errorMessage.set('');
    this.submitting.set(true);

    const raw = this.form.getRawValue();
    const recorrenteId = raw.transacaoRecorrenteId ? Number(raw.transacaoRecorrenteId) : null;

    const payload = {
      categoriaId: Number(raw.categoriaId),
      descricao: raw.descricao.trim(),
      valor:  Number(raw.valor),
      tipoMovimentacao: raw.tipoMovimentacao as TipoMovimentacao,
      tipoPagamento: raw.tipoPagamento as TipoPagamento,
      periodoId: periodo.id,
      transacaoRecorrenteId: recorrenteId,
      data: raw.data,
    };

    const editingId = this.editingId();
    const request$ = editingId
      ? this.transacoesApi.update(editingId, payload)
      : this.transacoesApi.create(payload);

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.show(editingId ? 'Transação atualizada.' : 'Transação criada.', 'success');
        this.saved.emit();
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

  formatDate = formatDate;

  private populateForm(tx: TransacaoResponse): void {
    this.form.setValue({
      transacaoRecorrenteId: tx.transacaoRecorrenteId ? String(tx.transacaoRecorrenteId) : '',
      categoriaId: String(tx.categoriaId),
      descricao: tx.descricao,
      valor: String(tx.valor),
      tipoMovimentacao: tx.tipoMovimentacao,
      tipoPagamento: tx.tipoPagamento,
      data: tx.data,
    });
    this.errorMessage.set('');
    this.toggleManualValidators(!tx.transacaoRecorrenteId);
  }

  private setInitialDate(): void {
    const periodo = this.selectedPeriodo();
    if (periodo) {
      const today = toIsoDate(new Date());
      this.form.controls.data.setValue(this.clampDateToPeriodo(today, periodo));
    }
    this.toggleManualValidators(true);
  }

  private toggleManualValidators(isManual: boolean): void {
    const manualControls = [
      this.form.controls.categoriaId,
      this.form.controls.tipoMovimentacao,
      this.form.controls.tipoPagamento,
    ];

    if (isManual) {
      manualControls.forEach((c) => {
        c.setValidators([Validators.required]);
        c.updateValueAndValidity({ emitEvent: false });
      });
      this.form.controls.descricao.setValidators([Validators.required, Validators.maxLength(255)]);
      this.form.controls.valor.setValidators([Validators.required, Validators.min(0.01)]);
    } else {
      manualControls.forEach((c) => {
        c.clearValidators();
        c.updateValueAndValidity({ emitEvent: false });
      });
      this.form.controls.descricao.setValidators([Validators.maxLength(255)]);
      this.form.controls.valor.clearValidators();
    }

    this.form.controls.descricao.updateValueAndValidity({ emitEvent: false });
    this.form.controls.valor.updateValueAndValidity({ emitEvent: false });
  }

  private clampDateToPeriodo(value: string, periodo: PeriodoFinanceiro): string {
    if (value < periodo.dataInicio) {
      return periodo.dataInicio;
    }
    if (value > periodo.dataFim) {
      return periodo.dataFim;
    }
    return value;
  }
}

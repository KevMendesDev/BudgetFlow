import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import {
  CategoriaResponse,
  CLASSIFICACAO_LABELS,
} from '../../../../core/models/categoria.models';
import { NaturezaFinanceira } from '../../../../core/models/natureza-financeira.models';
import { PeriodoFinanceiro } from '../../../../core/models/periodo-financeiro.models';
import { PlanejamentoResponse } from '../../../../core/models/planejamento.models';
import { TransacaoRecorrenteResponse } from '../../../../core/models/transacao-recorrente.models';
import {
  TIPOS_MOVIMENTACAO,
} from '../../../../core/models/transacao.models';
import { PlanejamentosApiService } from '../../../../core/services/planejamentos-api.service';
import { ToastService } from '../../../../core/services/toast.service';
import { mapApiError } from '../../../../shared/utils/error-message.util';
import { fieldError } from '../../../../shared/utils/form-error.util';

@Component({
  selector: 'app-planejamento-modal',
  imports: [ReactiveFormsModule],
  templateUrl: './planejamento-modal.component.html',
})
export class PlanejamentoModalComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly planejamentosApi = inject(PlanejamentosApiService);
  private readonly toast = inject(ToastService);

  readonly categorias = input.required<CategoriaResponse[]>();
  readonly recorrentes = input.required<TransacaoRecorrenteResponse[]>();
  readonly selectedPeriodo = input.required<PeriodoFinanceiro>();
  readonly editingPlanejamento = input<PlanejamentoResponse | null>(null);

  readonly saved = output<void>();
  readonly closed = output<void>();

  readonly tiposMovimentacao = TIPOS_MOVIMENTACAO;
  readonly classificacaoLabels = CLASSIFICACAO_LABELS;
  readonly fieldError = fieldError;
  readonly submitting = signal(false);
  readonly errorMessage = signal('');
  readonly editingId = computed(() => this.editingPlanejamento()?.id ?? null);

  readonly form = this.formBuilder.nonNullable.group({
    transacaoRecorrenteId: [''],
    categoriaId: ['', Validators.required],
    descricao: ['', [Validators.required, Validators.maxLength(255)]],
    valor: ['', [Validators.required, Validators.min(0.01)]],
    tipoMovimentacao: ['' as '' | NaturezaFinanceira, Validators.required],
  });

  ngOnInit(): void {
    const item = this.editingPlanejamento();
    if (item) {
      this.form.setValue({
        transacaoRecorrenteId: '',
        categoriaId: String(item.categoriaId),
        descricao: item.descricao,
        valor: String(item.valor),
        tipoMovimentacao: item.tipoMovimentacao,
      });
    }

    this.form.controls.tipoMovimentacao.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((tipo) => {
        const categoria = this.categorias().find(
          (item) => item.id === Number(this.form.controls.categoriaId.value)
        );
        if (categoria && categoria.tipoCategoria !== tipo) {
          this.form.controls.categoriaId.setValue('');
        }
      });
  }

  categoriasDisponiveis(): CategoriaResponse[] {
    const tipo = this.form.controls.tipoMovimentacao.value;
    return tipo ? this.categorias().filter((categoria) => categoria.tipoCategoria === tipo) : [];
  }

  onRecorrenteChange(rawId: string): void {
    const recorrente = this.recorrentes().find((item) => item.id === Number(rawId));
    if (!recorrente) {
      return;
    }

    this.form.patchValue({
      categoriaId: String(recorrente.categoriaId),
      descricao: recorrente.descricao,
      valor: recorrente.valorParcela == null ? '' : String(recorrente.valorParcela),
      tipoMovimentacao: recorrente.tipoMovimentacao,
    });
  }

  recorrenteLabel(recorrente: TransacaoRecorrenteResponse): string {
    return `${recorrente.descricao} - ${recorrente.categoriaNome}`;
  }

  categoriaLabel(categoria: CategoriaResponse): string {
    return categoria.classificacao
      ? `${categoria.nome} - ${this.classificacaoLabels[categoria.classificacao]}`
      : categoria.nome;
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const payload = {
      categoriaId: Number(raw.categoriaId),
      periodoId: this.selectedPeriodo().id,
      descricao: raw.descricao.trim(),
      valor: Number(raw.valor),
      tipoMovimentacao: raw.tipoMovimentacao as NaturezaFinanceira,
    };
    const editingId = this.editingId();
    const request$ = editingId
      ? this.planejamentosApi.update(editingId, payload)
      : this.planejamentosApi.create(payload);

    this.submitting.set(true);
    this.errorMessage.set('');
    request$.subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.show(editingId ? 'Planejamento atualizado.' : 'Planejamento criado.', 'success');
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
}

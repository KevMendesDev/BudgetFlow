import { Component, DestroyRef, inject, input, OnInit, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CategoriaResponse, CLASSIFICACAO_LABELS } from '../../../../core/models/categoria.models';
import { NaturezaFinanceira } from '../../../../core/models/natureza-financeira.models';
import {
  FREQUENCIAS,
  FREQUENCIA_LABELS,
  Frequencia,
  TransacaoRecorrenteResponse,
} from '../../../../core/models/transacao-recorrente.models';
import {
  TIPO_MOVIMENTACAO_LABELS,
  TIPO_PAGAMENTO_LABELS,
  TIPOS_MOVIMENTACAO,
  TIPOS_PAGAMENTO,
  TipoPagamento,
} from '../../../../core/models/transacao.models';
import { ToastService } from '../../../../core/services/toast.service';
import { TransacoesRecorrentesApiService } from '../../../../core/services/transacoes-recorrentes-api.service';
import { mapApiError } from '../../../../shared/utils/error-message.util';
import { fieldError } from '../../../../shared/utils/form-error.util';

@Component({
  selector: 'app-transacao-recorrente-modal',
  imports: [ReactiveFormsModule],
  templateUrl: './transacao-recorrente-modal.component.html',
})
export class TransacaoRecorrenteModalComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly recorrentesApi = inject(TransacoesRecorrentesApiService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly categorias = input.required<CategoriaResponse[]>();
  readonly loadingCategorias = input(false);
  readonly editingRecorrencia = input<TransacaoRecorrenteResponse | null>(null);

  readonly saved = output<void>();
  readonly closed = output<void>();

  readonly frequencias = FREQUENCIAS;
  readonly tiposMovimentacao = TIPOS_MOVIMENTACAO;
  readonly tiposPagamento = TIPOS_PAGAMENTO;
  readonly fieldError = fieldError;

  readonly submitting = signal(false);
  readonly errorMessage = signal('');
  readonly categoriasDisponiveis = signal<CategoriaResponse[]>([]);

  readonly form = this.formBuilder.nonNullable.group({
    categoriaId: ['', [Validators.required]],
    descricao: ['', [Validators.required, Validators.maxLength(255)]],
    valorParcela: [''],
    tipoMovimentacao: ['' as '' | NaturezaFinanceira, [Validators.required]],
    tipoPagamento: ['' as '' | TipoPagamento, [Validators.required]],
    frequencia: ['' as '' | Frequencia, [Validators.required]],
    dataInicio: ['', [Validators.required]],
    dataFim: [''],
    totalParcelas: ['', [Validators.min(1)]],
  });

  ngOnInit(): void {
    const item = this.editingRecorrencia();
    if (item) {
      this.form.setValue({
        categoriaId: String(item.categoriaId),
        descricao: item.descricao,
        valorParcela: item.valorParcela == null ? '' : String(item.valorParcela),
        tipoMovimentacao: item.tipoMovimentacao,
        tipoPagamento: item.tipoPagamento,
        frequencia: item.frequencia,
        dataInicio: item.dataInicio,
        dataFim: item.dataFim ?? '',
        totalParcelas: item.totalParcelas ? String(item.totalParcelas) : '',
      });
    }

    this.syncCategoriasDisponiveis();

    this.form.controls.tipoMovimentacao.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((tipoMovimentacao) => {
        this.syncCategoriasDisponiveis();

        const categoriaId = Number(this.form.controls.categoriaId.value);
        if (!categoriaId || !tipoMovimentacao) {
          return;
        }

        const categoria = this.categorias().find((current) => current.id === categoriaId);
        if (categoria && categoria.tipoCategoria !== tipoMovimentacao) {
          this.form.controls.categoriaId.setValue('');
        }
      });
  }

  categoriaOptionLabel(categoria: CategoriaResponse): string {
    const parts = [categoria.nome];
    if (categoria.classificacao) {
      parts.push(CLASSIFICACAO_LABELS[categoria.classificacao]);
    }
    return parts.join(' • ');
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set('');
    this.submitting.set(true);

    const raw = this.form.getRawValue();
    const valorParcela = raw.valorParcela;
    const payload = {
      categoriaId: Number(raw.categoriaId),
      descricao: raw.descricao.trim(),
      valorParcela:
        valorParcela === '' || valorParcela == null || Number(valorParcela) === 0
          ? null
          : Number(valorParcela),
      tipoMovimentacao: raw.tipoMovimentacao as NaturezaFinanceira,
      tipoPagamento: raw.tipoPagamento as TipoPagamento,
      frequencia: raw.frequencia as Frequencia,
      dataInicio: raw.dataInicio,
      dataFim: raw.dataFim || null,
      totalParcelas: raw.totalParcelas ? Number(raw.totalParcelas) : null,
    };

    const editingId = this.editingRecorrencia()?.id;
    const request$ = editingId
      ? this.recorrentesApi.update(editingId, payload)
      : this.recorrentesApi.create(payload);

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.show(editingId ? 'Recorrência atualizada.' : 'Recorrência criada.', 'success');
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

  private syncCategoriasDisponiveis(): void {
    const tipoMovimentacao = this.form.controls.tipoMovimentacao.value;
    if (!tipoMovimentacao) {
      this.categoriasDisponiveis.set([]);
      return;
    }

    this.categoriasDisponiveis.set(
      this.categorias().filter((categoria) => categoria.tipoCategoria === tipoMovimentacao)
    );
  }
}

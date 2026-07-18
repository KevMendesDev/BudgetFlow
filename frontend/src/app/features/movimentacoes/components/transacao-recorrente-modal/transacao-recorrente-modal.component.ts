import { Component, DestroyRef, effect, inject, input, OnInit, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CategoriaResponse, CLASSIFICACAO_LABELS } from '../../../../core/models/categoria.models';
import { NaturezaFinanceira } from '../../../../core/models/natureza-financeira.models';
import {
  FREQUENCIAS,
  Frequencia,
  STATUS_RECORRENCIA_EDITAVEIS,
  StatusRecorrencia,
  TransacaoRecorrenteResponse,
} from '../../../../core/models/transacao-recorrente.models';
import {
  TIPOS_MOVIMENTACAO,
  TIPOS_PAGAMENTO,
  TipoPagamento,
} from '../../../../core/models/transacao.models';
import { ToastService } from '../../../../core/services/toast.service';
import { TransacoesRecorrentesApiService } from '../../../../core/services/transacoes-recorrentes-api.service';
import { CurrencyInputDirective } from '../../../../shared/directives/currency-input.directive';
import { mapApiError } from '../../../../shared/utils/error-message.util';
import { fieldError } from '../../../../shared/utils/form-error.util';
import { parseCurrencyInput, toCurrencyInputValue } from '../../../../shared/utils/format.util';
import { currencyAmountValidator } from '../../../../shared/validators/br-validators';
import { CategoriaModalComponent } from '../../../categorias/components/categoria-modal/categoria-modal.component';

const CRIAR_NOVA_CATEGORIA = 'CRIAR_NOVA';

@Component({
  selector: 'app-transacao-recorrente-modal',
  imports: [ReactiveFormsModule, CategoriaModalComponent, CurrencyInputDirective],
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
  readonly categoriasChanged = output<CategoriaResponse>();

  readonly frequencias = FREQUENCIAS;
  readonly statusEditaveis = STATUS_RECORRENCIA_EDITAVEIS;
  readonly tiposMovimentacao = TIPOS_MOVIMENTACAO;
  readonly tiposPagamento = TIPOS_PAGAMENTO;
  readonly fieldError = fieldError;
  readonly criarNovaCategoriaValue = CRIAR_NOVA_CATEGORIA;

  readonly submitting = signal(false);
  readonly errorMessage = signal('');
  readonly categoriasDisponiveis = signal<CategoriaResponse[]>([]);
  readonly categoriasLocais = signal<CategoriaResponse[]>([]);
  readonly categoriaModalOpen = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    categoriaId: ['', [Validators.required]],
    descricao: ['', [Validators.required, Validators.maxLength(255)]],
    valorParcela: ['', [currencyAmountValidator(0.01, true)]],
    tipoMovimentacao: ['' as '' | NaturezaFinanceira, [Validators.required]],
    tipoPagamento: ['' as '' | TipoPagamento, [Validators.required]],
    frequencia: ['' as '' | Frequencia, [Validators.required]],
    dataInicio: ['', [Validators.required]],
    dataFim: [''],
    totalParcelas: ['', [Validators.min(1)]],
    status: ['ATIVA' as StatusRecorrencia, [Validators.required]],
  });

  constructor() {
    effect(() => {
      this.categoriasLocais.set(this.categorias());
      this.syncCategoriasDisponiveis();
    });
  }

  ngOnInit(): void {
    const item = this.editingRecorrencia();
    if (item) {
      this.form.setValue({
        categoriaId: String(item.categoriaId),
        descricao: item.descricao,
        valorParcela: toCurrencyInputValue(item.valorParcela),
        tipoMovimentacao: item.tipoMovimentacao,
        tipoPagamento: item.tipoPagamento,
        frequencia: item.frequencia,
        dataInicio: item.dataInicio,
        dataFim: item.dataFim ?? '',
        totalParcelas: item.totalParcelas ? String(item.totalParcelas) : '',
        status: item.status === 'FINALIZADA' ? 'ATIVA' : item.status,
      });

      if (item.status === 'FINALIZADA') {
        this.form.disable();
        this.errorMessage.set('Recorrência finalizada não pode ser alterada.');
      }
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

        const categoria = this.categoriasLocais().find((current) => current.id === categoriaId);
        if (categoria && categoria.tipoCategoria !== tipoMovimentacao) {
          this.form.controls.categoriaId.setValue('');
        }
      });
  }

  onCategoriaChange(rawValue: string): void {
    if (rawValue !== CRIAR_NOVA_CATEGORIA) {
      return;
    }

    this.form.controls.categoriaId.setValue('');
    this.categoriaModalOpen.set(true);
  }

  closeCategoriaModal(): void {
    this.categoriaModalOpen.set(false);
  }

  onCategoriaSaved(categoria: CategoriaResponse): void {
    this.categoriasLocais.update((lista) =>
      lista.some((item) => item.id === categoria.id) ? lista : [...lista, categoria]
    );
    this.syncCategoriasDisponiveis();
    this.form.controls.categoriaId.setValue(String(categoria.id));
    this.categoriasChanged.emit(categoria);
    this.closeCategoriaModal();
  }

  tipoCategoriaInicial(): NaturezaFinanceira | null {
    const tipo = this.form.controls.tipoMovimentacao.value;
    return tipo || null;
  }

  categoriaOptionLabel(categoria: CategoriaResponse): string {
    const parts = [categoria.nome];
    if (categoria.classificacao) {
      parts.push(CLASSIFICACAO_LABELS[categoria.classificacao]);
    }
    return parts.join(' • ');
  }

  submit(): void {
    if (this.editingRecorrencia()?.status === 'FINALIZADA') {
      this.errorMessage.set('Recorrência finalizada não pode ser alterada.');
      return;
    }

    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set('');
    this.submitting.set(true);

    const raw = this.form.getRawValue();
    const valorParcela = parseCurrencyInput(raw.valorParcela);
    const payload = {
      categoriaId: Number(raw.categoriaId),
      descricao: raw.descricao.trim(),
      valorParcela: valorParcela == null || valorParcela === 0 ? null : valorParcela,
      tipoMovimentacao: raw.tipoMovimentacao as NaturezaFinanceira,
      tipoPagamento: raw.tipoPagamento as TipoPagamento,
      frequencia: raw.frequencia as Frequencia,
      dataInicio: raw.dataInicio,
      dataFim: raw.dataFim || null,
      totalParcelas: raw.totalParcelas ? Number(raw.totalParcelas) : null,
      status: raw.status as StatusRecorrencia,
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
      this.categoriasLocais().filter((categoria) => categoria.tipoCategoria === tipoMovimentacao)
    );
  }
}

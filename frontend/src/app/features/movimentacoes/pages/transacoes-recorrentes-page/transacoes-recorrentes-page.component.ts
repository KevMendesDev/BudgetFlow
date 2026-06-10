import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import {
  CategoriaResponse,
  CLASSIFICACAO_LABELS,
} from '../../../../core/models/categoria.models';
import { NaturezaFinanceira } from '../../../../core/models/natureza-financeira.models';
import {
  FREQUENCIA_LABELS,
  FREQUENCIAS,
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
import { CategoriasApiService } from '../../../../core/services/categorias-api.service';
import { ToastService } from '../../../../core/services/toast.service';
import { TransacoesRecorrentesApiService } from '../../../../core/services/transacoes-recorrentes-api.service';
import { ConfirmDialogService } from '../../../../shared/services/confirm-dialog.service';
import { CurrencyBRLPipe } from '../../../../shared/pipes/currency-brl.pipe';
import { DateBRPipe } from '../../../../shared/pipes/date-br.pipe';
import { mapApiError } from '../../../../shared/utils/error-message.util';
import { fieldError } from '../../../../shared/utils/form-error.util';
import { isDesktopViewport } from '../../../../shared/utils/viewport.util';

@Component({
  selector: 'app-transacoes-recorrentes-page',
  imports: [ReactiveFormsModule, CurrencyBRLPipe, DateBRPipe],
  templateUrl: './transacoes-recorrentes-page.component.html',
  styleUrl: './transacoes-recorrentes-page.component.scss',
})
export class TransacoesRecorrentesPageComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly recorrentesApi = inject(TransacoesRecorrentesApiService);
  private readonly categoriasApi = inject(CategoriasApiService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly destroyRef = inject(DestroyRef);

  readonly recorrencias = signal<TransacaoRecorrenteResponse[]>([]);
  readonly categorias = signal<CategoriaResponse[]>([]);
  readonly loading = signal(false);
  readonly loadingCategorias = signal(false);
  readonly submitting = signal(false);
  readonly deletingId = signal<number | null>(null);
  readonly modalOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly errorMessage = signal('');
  readonly modalErrorMessage = signal('');
  readonly filtrosAbertos = signal(isDesktopViewport());

  readonly frequencias = FREQUENCIAS;
  readonly tiposMovimentacao = TIPOS_MOVIMENTACAO;
  readonly tiposPagamento = TIPOS_PAGAMENTO;
  readonly fieldError = fieldError;
  readonly classificacaoLabels = CLASSIFICACAO_LABELS;

  readonly filtersForm = this.formBuilder.nonNullable.group({
    query: [''],
    frequencia: ['' as '' | Frequencia],
    tipoMovimentacao: ['' as '' | NaturezaFinanceira],
  });

  readonly form = this.formBuilder.nonNullable.group({
    categoriaId: ['', [Validators.required]],
    descricao: ['', [Validators.required, Validators.maxLength(255)]],
    valorParcela: ['', [Validators.required, Validators.min(0.01)]],
    tipoMovimentacao: ['' as '' | NaturezaFinanceira, [Validators.required]],
    tipoPagamento: ['' as '' | TipoPagamento, [Validators.required]],
    frequencia: ['' as '' | Frequencia, [Validators.required]],
    dataInicio: ['', [Validators.required]],
    dataFim: [''],
    totalParcelas: ['', [Validators.min(1)]],
  });

  readonly categoriasDisponiveis = signal<CategoriaResponse[]>([]);

  ngOnInit(): void {
    this.loadCategorias();
    this.loadRecorrencias();
    this.syncCategoriasDisponiveis();

    this.filtersForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => this.loadRecorrencias());

    this.form.controls.tipoMovimentacao.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((tipoMovimentacao) => {
        this.syncCategoriasDisponiveis();

        const categoriaId = Number(this.form.controls.categoriaId.value);
        if (!categoriaId || !tipoMovimentacao) {
          return;
        }

        const categoria = this.categorias().find((item) => item.id === categoriaId);
        if (categoria && categoria.tipoCategoria !== tipoMovimentacao) {
          this.form.controls.categoriaId.setValue('');
        }
      });
  }

  toggleFiltros(): void {
    this.filtrosAbertos.update((v) => !v);
  }

  applyFilters(): void {
    this.loadRecorrencias();
  }

  clearFilters(): void {
    this.filtersForm.setValue({ query: '', frequencia: '', tipoMovimentacao: '' }, { emitEvent: false });
    this.loadRecorrencias();
  }

  openCreateModal(): void {
    this.modalOpen.set(true);
    this.resetForm();
  }

  startEdit(item: TransacaoRecorrenteResponse): void {
    this.modalOpen.set(true);
    this.editingId.set(item.id);
    this.form.setValue({
      categoriaId: String(item.categoriaId),
      descricao: item.descricao,
      valorParcela: String(item.valorParcela),
      tipoMovimentacao: item.tipoMovimentacao,
      tipoPagamento: item.tipoPagamento,
      frequencia: item.frequencia,
      dataInicio: item.dataInicio,
      dataFim: item.dataFim ?? '',
      totalParcelas: item.totalParcelas ? String(item.totalParcelas) : '',
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

    this.modalErrorMessage.set('');
    this.submitting.set(true);

    const raw = this.form.getRawValue();
    const payload = {
      categoriaId: Number(raw.categoriaId),
      descricao: raw.descricao.trim(),
      valorParcela: Number(raw.valorParcela),
      tipoMovimentacao: raw.tipoMovimentacao as NaturezaFinanceira,
      tipoPagamento: raw.tipoPagamento as TipoPagamento,
      frequencia: raw.frequencia as Frequencia,
      dataInicio: raw.dataInicio,
      dataFim: raw.dataFim || null,
      totalParcelas: raw.totalParcelas ? Number(raw.totalParcelas) : null,
    };

    const editingId = this.editingId();
    const request$ = editingId
      ? this.recorrentesApi.update(editingId, payload)
      : this.recorrentesApi.create(payload);

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.show(editingId ? 'Recorrência atualizada.' : 'Recorrência criada.', 'success');
        this.closeModal();
        this.loadRecorrencias();
      },
      error: (err) => {
        this.modalErrorMessage.set(mapApiError(err));
        this.submitting.set(false);
      },
    });
  }

  async deleteRecorrencia(item: TransacaoRecorrenteResponse): Promise<void> {
    if (this.deletingId()) {
      return;
    }

    const confirmed = await this.confirmDialog.confirm(
      `Excluir a recorrência "${item.descricao}"?`
    );
    if (!confirmed) {
      return;
    }

    this.deletingId.set(item.id);

    this.recorrentesApi
      .delete(item.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.deletingId.set(null);
          this.toast.show('Recorrência excluída.', 'success');
          this.loadRecorrencias();
        },
        error: () => {
          this.deletingId.set(null);
        },
      });
  }

  frequenciaLabel(value: Frequencia): string {
    return FREQUENCIA_LABELS[value] ?? value;
  }

  tipoMovimentacaoLabel(value: NaturezaFinanceira): string {
    return TIPO_MOVIMENTACAO_LABELS[value] ?? value;
  }

  tipoPagamentoLabel(value: TipoPagamento): string {
    return TIPO_PAGAMENTO_LABELS[value] ?? value;
  }

  categoriaOptionLabel(categoria: CategoriaResponse): string {
    const parts = [categoria.nome];
    if (categoria.classificacao) {
      parts.push(this.classificacaoLabels[categoria.classificacao]);
    }
    return parts.join(' • ');
  }

  private loadCategorias(): void {
    this.loadingCategorias.set(true);

    this.categoriasApi
      .listAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.categorias.set(page.content);
          this.syncCategoriasDisponiveis();
          this.loadingCategorias.set(false);
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
          this.loadingCategorias.set(false);
        },
      });
  }

  private loadRecorrencias(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    const filters = this.filtersForm.getRawValue();

    this.recorrentesApi
      .listAll({
        query: filters.query,
        frequencia: filters.frequencia,
        tipoMovimentacao: filters.tipoMovimentacao,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.recorrencias.set(page.content);
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
    this.form.setValue({
      categoriaId: '',
      descricao: '',
      valorParcela: '',
      tipoMovimentacao: '',
      tipoPagamento: '',
      frequencia: '',
      dataInicio: '',
      dataFim: '',
      totalParcelas: '',
    });
    this.form.markAsPristine();
    this.form.markAsUntouched();
    this.modalErrorMessage.set('');
    this.syncCategoriasDisponiveis();
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

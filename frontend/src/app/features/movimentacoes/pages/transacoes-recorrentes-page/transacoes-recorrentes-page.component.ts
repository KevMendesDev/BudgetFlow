import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import {
  CategoriaResponse,
} from '../../../../core/models/categoria.models';
import { NaturezaFinanceira } from '../../../../core/models/natureza-financeira.models';
import { PageSize } from '../../../../core/models/pagination.models';
import {
  FREQUENCIA_LABELS,
  FREQUENCIAS,
  Frequencia,
  STATUS_RECORRENCIA,
  STATUS_RECORRENCIA_LABELS,
  StatusRecorrencia,
  TransacaoRecorrenteResponse,
} from '../../../../core/models/transacao-recorrente.models';
import {
  TIPO_MOVIMENTACAO_LABELS,
  TIPO_PAGAMENTO_LABELS,
  TIPOS_MOVIMENTACAO,
  TipoPagamento,
} from '../../../../core/models/transacao.models';
import { CategoriasApiService } from '../../../../core/services/categorias-api.service';
import { ToastService } from '../../../../core/services/toast.service';
import { TransacoesRecorrentesApiService } from '../../../../core/services/transacoes-recorrentes-api.service';
import { ConfirmDialogService } from '../../../../shared/services/confirm-dialog.service';
import { CurrencyBRLPipe } from '../../../../shared/pipes/currency-brl.pipe';
import { DateBRPipe } from '../../../../shared/pipes/date-br.pipe';
import { mapApiError } from '../../../../shared/utils/error-message.util';
import { isDesktopViewport } from '../../../../shared/utils/viewport.util';
import { TransacaoRecorrenteModalComponent } from '../../components/transacao-recorrente-modal/transacao-recorrente-modal.component';

@Component({
  selector: 'app-transacoes-recorrentes-page',
  imports: [ReactiveFormsModule, CurrencyBRLPipe, DateBRPipe, TransacaoRecorrenteModalComponent],
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
  readonly deletingId = signal<number | null>(null);
  readonly modalOpen = signal(false);
  readonly editingRecorrencia = signal<TransacaoRecorrenteResponse | null>(null);
  readonly errorMessage = signal('');
  readonly filtrosAbertos = signal(isDesktopViewport());
  readonly paginaAtual = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  readonly frequencias = FREQUENCIAS;
  readonly statusRecorrencia = STATUS_RECORRENCIA;
  readonly tiposMovimentacao = TIPOS_MOVIMENTACAO;
  readonly filtersForm = this.formBuilder.nonNullable.group({
    query: [''],
    frequencia: ['' as '' | Frequencia],
    tipoMovimentacao: ['' as '' | NaturezaFinanceira],
    status: ['' as '' | StatusRecorrencia],
  });

  ngOnInit(): void {
    this.loadCategorias();
    this.loadRecorrencias(0);

    this.filtersForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => this.loadRecorrencias(0));

  }

  toggleFiltros(): void {
    this.filtrosAbertos.update((v) => !v);
  }

  applyFilters(): void {
    this.loadRecorrencias(0);
  }

  clearFilters(): void {
    this.filtersForm.setValue(
      { query: '', frequencia: '', tipoMovimentacao: '', status: '' },
      { emitEvent: false }
    );
    this.loadRecorrencias(0);
  }

  openCreateModal(): void {
    this.editingRecorrencia.set(null);
    this.modalOpen.set(true);
  }

  startEdit(item: TransacaoRecorrenteResponse): void {
    if (item.status === 'FINALIZADA') {
      this.toast.show('Recorrência finalizada não pode ser alterada.', 'error');
      return;
    }
    this.editingRecorrencia.set(item);
    this.modalOpen.set(true);
  }

  closeModal(): void {
    this.modalOpen.set(false);
    this.editingRecorrencia.set(null);
  }

  onRecorrenciaSaved(): void {
    this.closeModal();
    this.loadRecorrencias(this.paginaAtual());
  }

  onCategoriasChanged(): void {
    this.loadCategorias();
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
          this.loadRecorrencias(this.paginaAtual());
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

  statusLabel(value: StatusRecorrencia): string {
    return STATUS_RECORRENCIA_LABELS[value] ?? value;
  }

  statusBadgeClass(value: StatusRecorrencia): string {
    switch (value) {
      case 'ATIVA':
        return 'card-badge card-badge--ativa';
      case 'INATIVA':
        return 'card-badge card-badge--inativa';
      case 'FINALIZADA':
        return 'card-badge card-badge--finalizada';
      default:
        return 'card-badge';
    }
  }

  goToPreviousPage(): void {
    const atual = this.paginaAtual();
    if (atual > 0) {
      this.loadRecorrencias(atual - 1);
    }
  }

  goToNextPage(): void {
    const atual = this.paginaAtual();
    if (atual + 1 < this.totalPages()) {
      this.loadRecorrencias(atual + 1);
    }
  }

  private loadCategorias(): void {
    this.loadingCategorias.set(true);

    this.categoriasApi
      .listAll({ size: PageSize.LARGE })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.categorias.set(page.content);
          this.loadingCategorias.set(false);
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
          this.loadingCategorias.set(false);
        },
      });
  }

  private loadRecorrencias(page = this.paginaAtual()): void {
    this.loading.set(true);
    this.errorMessage.set('');

    const filters = this.filtersForm.getRawValue();

    this.recorrentesApi
      .listAll({
        page,
        size: PageSize.DEFAULT,
        query: filters.query,
        frequencia: filters.frequencia,
        tipoMovimentacao: filters.tipoMovimentacao,
        status: filters.status,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          const totalPages = page.page.totalPages ?? page.totalPages ?? 0;
          const totalElements = page.page.totalElements ?? page.totalElements ?? 0;

          if (page.content.length === 0 && totalPages > 0 && this.paginaAtual() >= totalPages) {
            this.loadRecorrencias(totalPages - 1);
            return;
          }

          this.recorrencias.set(page.content);
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

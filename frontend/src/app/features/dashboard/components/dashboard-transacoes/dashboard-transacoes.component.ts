import { Component, computed, DestroyRef, effect, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import {
  CategoriaResponse,
  ClassificacaoCategoria,
  CLASSIFICACOES,
} from '../../../../core/models/categoria.models';
import { PeriodoFinanceiro } from '../../../../core/models/periodo-financeiro.models';
import {
  TIPOS_MOVIMENTACAO,
  TIPOS_PAGAMENTO,
  TipoMovimentacao,
  TipoPagamento,
  TransacaoResponse,
} from '../../../../core/models/transacao.models';
import { TransacaoRecorrenteResponse, Frequencia } from '../../../../core/models/transacao-recorrente.models';
import { TransacoesApiService } from '../../../../core/services/transacoes-api.service';
import { ToastService } from '../../../../core/services/toast.service';
import { ConfirmDialogService } from '../../../../shared/services/confirm-dialog.service';
import { CurrencyBRLPipe } from '../../../../shared/pipes/currency-brl.pipe';
import { mapApiError } from '../../../../shared/utils/error-message.util';
import { formatDate } from '../../../../shared/utils/format.util';
import { isDesktopViewport } from '../../../../shared/utils/viewport.util';
import { TransacaoModalComponent } from '../transacao-modal/transacao-modal.component';

@Component({
  selector: 'app-dashboard-transacoes',
  imports: [CurrencyBRLPipe, TransacaoModalComponent],
  templateUrl: './dashboard-transacoes.component.html',
  styleUrl: './dashboard-transacoes.component.scss',
})
export class DashboardTransacoesComponent {
  private static readonly PAGE_SIZE = 20;

  private readonly transacoesApi = inject(TransacoesApiService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly destroyRef = inject(DestroyRef);

  readonly categorias = input.required<CategoriaResponse[]>();
  readonly recorrentes = input.required<TransacaoRecorrenteResponse[]>();
  readonly transacoes = input.required<TransacaoResponse[]>();
  readonly selectedPeriodo = input<PeriodoFinanceiro | null>(null);

  readonly changed = output<void>();

  readonly tiposMovimentacao = TIPOS_MOVIMENTACAO;
  readonly tiposPagamento = TIPOS_PAGAMENTO;
  readonly classificacoes = CLASSIFICACOES;

  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly deletingId = signal<number | null>(null);
  readonly modalOpen = signal(false);
  readonly editingTransacao = signal<TransacaoResponse | null>(null);

  readonly filtroNomeCategoria = signal('');
  readonly filtroClassificacao = signal<ClassificacaoCategoria | ''>('');
  readonly filtroTipoMovimentacao = signal<TipoMovimentacao | ''>('');
  readonly filtroTipoPagamento = signal<TipoPagamento | ''>('');
  readonly filtroRecorrente = signal<'all' | 'true' | 'false'>('all');
  readonly filtrosAbertos = signal(isDesktopViewport());

  readonly categoriasDistinct = computed(() => {
    const names = new Set<string>();
    this.categorias().forEach((categoria) => {
      if (categoria.nome?.trim()) {
        names.add(categoria.nome.trim());
      }
    });
    return Array.from(names).sort((a, b) => a.localeCompare(b));
  });

  readonly paginaAtual = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly paginaConteudo = signal<TransacaoResponse[]>([]);

  readonly skeletonRows = [1, 2, 3, 4, 5];

  constructor() {
    effect(() => {
      const periodo = this.selectedPeriodo();
      const transacoes = this.transacoes();
      if (!periodo) {
        this.resetPageState();
        return;
      }

      this.paginaAtual.set(0);

      if (this.shouldUseClientPagination()) {
        this.syncPageFromInputs(0, transacoes);
        return;
      }

      this.loadPage(0);
    });
  }

  formatDate = formatDate;

  openCreateModal(): void {
    this.editingTransacao.set(null);
    this.modalOpen.set(true);
  }

  startEdit(tx: TransacaoResponse): void {
    this.editingTransacao.set(tx);
    this.modalOpen.set(true);
  }

  closeModal(): void {
    this.modalOpen.set(false);
  }

  onTransacaoSaved(): void {
    this.modalOpen.set(false);
    this.changed.emit();

    if (!this.shouldUseClientPagination()) {
      this.loadPage(this.paginaAtual());
    }
  }

  async deleteTransacao(tx: TransacaoResponse): Promise<void> {
    if (this.deletingId()) {
      return;
    }

    const confirmed = await this.confirmDialog.confirm(
      `Excluir a transação "${tx.descricao || 'Sem descrição'}"?`
    );
    if (!confirmed) {
      return;
    }

    this.deletingId.set(tx.id);

    this.transacoesApi
      .delete(tx.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.deletingId.set(null);
          this.toast.show('Transação excluída.', 'success');
          this.changed.emit();

          if (!this.shouldUseClientPagination()) {
            this.loadPage(this.paginaAtual());
          }
        },
        error: () => {
          this.deletingId.set(null);
        },
      });
  }

  aplicarFiltros(): void {
    this.paginaAtual.set(0);

    if (this.shouldUseClientPagination()) {
      this.syncPageFromInputs(0);
      return;
    }

    this.loadPage(0);
  }

  toggleFiltros(): void {
    this.filtrosAbertos.update((value) => !value);
  }

  limparFiltros(): void {
    this.filtroNomeCategoria.set('');
    this.filtroClassificacao.set('');
    this.filtroTipoMovimentacao.set('');
    this.filtroTipoPagamento.set('');
    this.filtroRecorrente.set('all');
    this.paginaAtual.set(0);
    this.syncPageFromInputs(0);
  }

  irPaginaAnterior(): void {
    const atual = this.paginaAtual();
    if (atual <= 0) {
      return;
    }

    if (this.shouldUseClientPagination()) {
      this.syncPageFromInputs(atual - 1);
      return;
    }

    this.paginaAtual.set(atual - 1);
    this.loadPage(atual - 1);
  }

  irProximaPagina(): void {
    const atual = this.paginaAtual();
    if (atual + 1 >= this.totalPages()) {
      return;
    }

    if (this.shouldUseClientPagination()) {
      this.syncPageFromInputs(atual + 1);
      return;
    }

    this.paginaAtual.set(atual + 1);
    this.loadPage(atual + 1);
  }

  parcelaInfo(tx: TransacaoResponse): string {
    if (!tx.transacaoRecorrenteId) {
      return '';
    }

    const recorrente = this.recorrentes().find((item) => item.id === tx.transacaoRecorrenteId);
    if (!recorrente) {
      return '';
    }

    const total = recorrente.totalParcelas ?? this.calcularTotalParcelasPorDataFim(recorrente);
    if (!total) {
      return '';
    }

    const numero = this.calcularNumeroParcela(recorrente, tx.data);
    if (!numero) {
      return '';
    }

    return `${numero}/${total}`;
  }

  private loadPage(page: number): void {
    const params = this.buildListParams(page);
    if (!params) {
      return;
    }

    this.startLoadingPage();

    this.transacoesApi
      .listByPeriodoFiltered(params)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (pageResponse) => {
          this.paginaConteudo.set(pageResponse.content);
          this.totalPages.set(pageResponse.page.totalPages ?? 0);
          this.totalElements.set(pageResponse.page.totalElements ?? 0);
          this.loading.set(false);
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
          this.loading.set(false);
        },
      });
  }

  private startLoadingPage(): void {
    this.loading.set(true);
    this.errorMessage.set('');
  }

  private resetPageState(): void {
    this.loading.set(false);
    this.errorMessage.set('');
    this.paginaAtual.set(0);
    this.paginaConteudo.set([]);
    this.totalPages.set(0);
    this.totalElements.set(0);
  }

  private shouldUseClientPagination(): boolean {
    return !!this.selectedPeriodo() && !this.hasActiveFilters();
  }

  private hasActiveFilters(): boolean {
    return !!(
      this.filtroNomeCategoria().trim() ||
      this.filtroClassificacao() ||
      this.filtroTipoMovimentacao() ||
      this.filtroTipoPagamento() ||
      this.filtroRecorrente() !== 'all'
    );
  }

  private syncPageFromInputs(page: number, transacoes = this.transacoes()): void {
    const totalElements = transacoes.length;
    const totalPages = Math.max(1, Math.ceil(totalElements / DashboardTransacoesComponent.PAGE_SIZE));
    const currentPage = Math.min(page, totalPages - 1);
    const start = currentPage * DashboardTransacoesComponent.PAGE_SIZE;
    const end = start + DashboardTransacoesComponent.PAGE_SIZE;

    this.errorMessage.set('');
    this.loading.set(false);
    this.paginaAtual.set(currentPage);
    this.totalElements.set(totalElements);
    this.totalPages.set(totalElements === 0 ? 0 : totalPages);
    this.paginaConteudo.set(transacoes.slice(start, end));
  }

  private buildListParams(page: number): {
    periodoId: number;
    dataInicio: string;
    dataFim: string;
    page: number;
    size: number;
    nomeCategoria?: string;
    classificacaoCategoria?: ClassificacaoCategoria;
    tipoMovimentacao?: TipoMovimentacao;
    tipoPagamento?: TipoPagamento;
    recorrente?: boolean;
  } | null {
    const periodo = this.selectedPeriodo();
    if (!periodo) {
      return null;
    }

    const nomeCategoria = this.filtroNomeCategoria().trim();
    const classificacao = this.filtroClassificacao();
    const tipoMovimentacao = this.filtroTipoMovimentacao();
    const tipoPagamento = this.filtroTipoPagamento();
    const recorrenteRaw = this.filtroRecorrente();

    return {
      periodoId: periodo.id,
      dataInicio: periodo.dataInicio,
      dataFim: periodo.dataFim,
      page,
      size: DashboardTransacoesComponent.PAGE_SIZE,
      nomeCategoria: nomeCategoria || undefined,
      classificacaoCategoria: classificacao || undefined,
      tipoMovimentacao: tipoMovimentacao || undefined,
      tipoPagamento: tipoPagamento || undefined,
      recorrente: recorrenteRaw === 'all' ? undefined : recorrenteRaw === 'true',
    };
  }

  private calcularNumeroParcela(
    recorrente: TransacaoRecorrenteResponse,
    data: string
  ): number | null {
    const inicio = new Date(`${recorrente.dataInicio}T00:00:00`);
    const alvo = new Date(`${data}T00:00:00`);

    if (alvo < inicio) {
      return null;
    }

    const indice = this.calcularIndice(inicio, alvo, recorrente.frequencia);
    return indice >= 0 ? indice + 1 : null;
  }

  private calcularTotalParcelasPorDataFim(recorrente: TransacaoRecorrenteResponse): number | null {
    if (!recorrente.dataFim) {
      return null;
    }

    const inicio = new Date(`${recorrente.dataInicio}T00:00:00`);
    const fim = new Date(`${recorrente.dataFim}T00:00:00`);

    if (fim < inicio) {
      return null;
    }

    return this.calcularIndice(inicio, fim, recorrente.frequencia) + 1;
  }

  private calcularIndice(inicio: Date, alvo: Date, frequencia: Frequencia): number {
    const MS_DIA = 1000 * 60 * 60 * 24;

    switch (frequencia) {
      case 'DIARIO':
        return Math.floor((alvo.getTime() - inicio.getTime()) / MS_DIA);
      case 'SEMANAL':
        return Math.floor((alvo.getTime() - inicio.getTime()) / (MS_DIA * 7));
      case 'MENSAL':
        return (alvo.getFullYear() - inicio.getFullYear()) * 12 + (alvo.getMonth() - inicio.getMonth());
      case 'ANUAL':
        return alvo.getFullYear() - inicio.getFullYear();
    }
  }
}

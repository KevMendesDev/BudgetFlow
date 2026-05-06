import { Component, computed, DestroyRef, effect, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import {
  CategoriaResponse,
  ClassificacaoCategoria,
  CLASSIFICACOES,
} from '../../../../core/models/categoria.models';
import { PeriodoFinanceiro } from '../../../../core/models/periodo.models';
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
import { formatDate, toIsoDate } from '../../../../shared/utils/format.util';
import { TransacaoModalComponent } from '../transacao-modal/transacao-modal.component';

@Component({
  selector: 'app-dashboard-transacoes',
  imports: [CurrencyBRLPipe, TransacaoModalComponent],
  templateUrl: './dashboard-transacoes.component.html',
  styleUrl: './dashboard-transacoes.component.scss',
})
export class DashboardTransacoesComponent {
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
  readonly filtrosAbertos = signal(true);

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
      if (!periodo) {
        this.loading.set(false);
        this.errorMessage.set('');
        this.paginaConteudo.set([]);
        this.totalPages.set(0);
        this.totalElements.set(0);
        return;
      }
      this.paginaAtual.set(0);
      this.carregarPagina(0);
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
    this.carregarPagina(this.paginaAtual());
    this.changed.emit();
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
          this.carregarPagina(this.paginaAtual());
          this.changed.emit();
        },
        error: (err) => {
          this.deletingId.set(null);
          this.toast.show(mapApiError(err), 'error');
        },
      });
  }

  aplicarFiltros(): void {
    this.paginaAtual.set(0);
    this.carregarPagina(0);
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
    this.carregarPagina(0);
  }

  irPaginaAnterior(): void {
    const atual = this.paginaAtual();
    if (atual <= 0) {
      return;
    }
    this.paginaAtual.set(atual - 1);
    this.carregarPagina(atual - 1);
  }

  irProximaPagina(): void {
    const atual = this.paginaAtual();
    if (atual + 1 >= this.totalPages()) {
      return;
    }
    this.paginaAtual.set(atual + 1);
    this.carregarPagina(atual + 1);
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

  private carregarPagina(page: number): void {
    const periodo = this.selectedPeriodo();
    if (!periodo) {
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    const nomeCategoria = this.filtroNomeCategoria().trim();
    const classificacao = this.filtroClassificacao();
    const tipoMovimentacao = this.filtroTipoMovimentacao();
    const tipoPagamento = this.filtroTipoPagamento();
    const recorrenteRaw = this.filtroRecorrente();

    this.transacoesApi
      .listByPeriodoFiltered({
        periodoId: periodo.id,
        dataInicio: periodo.dataInicio,
        dataFim: periodo.dataFim,
        page,
        size: 20,
        nomeCategoria: nomeCategoria || undefined,
        classificacaoCategoria: classificacao || undefined,
        tipoMovimentacao: tipoMovimentacao || undefined,
        tipoPagamento: tipoPagamento || undefined,
        recorrente: recorrenteRaw === 'all' ? undefined : recorrenteRaw === 'true',
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (pageResponse) => {
          this.paginaConteudo.set(pageResponse.content);
          this.totalPages.set(pageResponse.totalPages ?? 0);
          this.totalElements.set(pageResponse.totalElements ?? 0);
          this.loading.set(false);
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
          this.loading.set(false);
        },
      });
  }

  private calcularNumeroParcela(
    recorrente: TransacaoRecorrenteResponse,
    data: string
  ): number | null {
    let atual = recorrente.dataInicio;
    let indice = 1;
    let guard = 0;

    while (atual <= data && guard < 5000) {
      if (atual === data) {
        return indice;
      }
      atual = this.proximaData(atual, recorrente.frequencia);
      indice += 1;
      guard += 1;
    }

    return null;
  }

  private calcularTotalParcelasPorDataFim(recorrente: TransacaoRecorrenteResponse): number | null {
    if (!recorrente.dataFim) {
      return null;
    }

    let atual = recorrente.dataInicio;
    let total = 0;
    let guard = 0;

    while (atual <= recorrente.dataFim && guard < 5000) {
      total += 1;
      atual = this.proximaData(atual, recorrente.frequencia);
      guard += 1;
    }

    return total || null;
  }

  private proximaData(dataIso: string, frequencia: Frequencia): string {
    const date = new Date(`${dataIso}T00:00:00`);

    if (frequencia === 'DIARIO') {
      date.setDate(date.getDate() + 1);
    } else if (frequencia === 'SEMANAL') {
      date.setDate(date.getDate() + 7);
    } else if (frequencia === 'MENSAL') {
      const day = date.getDate();
      const next = new Date(date);
      next.setDate(1);
      next.setMonth(next.getMonth() + 1);
      const maxDay = new Date(next.getFullYear(), next.getMonth() + 1, 0).getDate();
      next.setDate(Math.min(day, maxDay));
      return toIsoDate(next);
    } else {
      const day = date.getDate();
      const month = date.getMonth();
      const year = date.getFullYear() + 1;
      const maxDay = new Date(year, month + 1, 0).getDate();
      return toIsoDate(new Date(year, month, Math.min(day, maxDay)));
    }

    return toIsoDate(date);
  }
}

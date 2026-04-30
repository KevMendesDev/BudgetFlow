import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { CategoriaResponse } from '../../../../core/models/categoria.models';
import { PeriodoFinanceiro } from '../../../../core/models/periodo.models';
import { TransacaoRecorrenteResponse } from '../../../../core/models/transacao-recorrente.models';
import { SessionService } from '../../../../core/services/session.service';
import { TransacaoResponse } from '../../../../core/models/transacao.models';
import { CategoriasApiService } from '../../../../core/services/categorias-api.service';
import { PeriodosApiService } from '../../../../core/services/periodos-api.service';
import { TransacoesApiService } from '../../../../core/services/transacoes-api.service';
import { TransacoesRecorrentesApiService } from '../../../../core/services/transacoes-recorrentes-api.service';
import { ToastService } from '../../../../core/services/toast.service';
import { ConfirmDialogService } from '../../../../shared/services/confirm-dialog.service';
import { CurrencyBRLPipe } from '../../../../shared/pipes/currency-brl.pipe';
import { mapApiError } from '../../../../shared/utils/error-message.util';
import { formatDate, toIsoDate } from '../../../../shared/utils/format.util';
import { TransacaoModalComponent } from '../../components/transacao-modal/transacao-modal.component';
import { Frequencia } from '../../../../core/models/transacao-recorrente.models';

@Component({
  selector: 'app-dashboard-page',
  imports: [CurrencyBRLPipe, TransacaoModalComponent],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
})
export class DashboardPageComponent implements OnInit {
  private readonly categoriasApi = inject(CategoriasApiService);
  private readonly periodosApi = inject(PeriodosApiService);
  private readonly session = inject(SessionService);
  private readonly toast = inject(ToastService);
  private readonly transacoesApi = inject(TransacoesApiService);
  private readonly transacoesRecorrentesApi = inject(TransacoesRecorrentesApiService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly destroyRef = inject(DestroyRef);

  readonly user = computed(() => this.session.user());
  readonly loadingPeriodos = signal(true);
  readonly loadingResumo = signal(true);
  readonly loadingRecorrentes = signal(true);
  readonly loadingCategorias = signal(true);
  readonly loading = computed(() => this.loadingPeriodos() || this.loadingResumo());
  readonly deletingId = signal<number | null>(null);
  readonly modalOpen = signal(false);
  readonly editingTransacao = signal<TransacaoResponse | null>(null);
  readonly errorMessage = signal('');
  readonly periodos = signal<PeriodoFinanceiro[]>([]);
  readonly categorias = signal<CategoriaResponse[]>([]);
  readonly recorrentes = signal<TransacaoRecorrenteResponse[]>([]);
  readonly selectedPeriodoId = signal<number | null>(null);
  readonly transacoes = signal<TransacaoResponse[]>([]);

  readonly skeletonCards = [1, 2, 3];
  readonly skeletonRows = [1, 2, 3, 4, 5, 6];

  readonly selectedPeriodo = computed(() => {
    const id = this.selectedPeriodoId();
    return this.periodos().find((periodo) => periodo.id === id) ?? null;
  });

  readonly resumo = computed(() => {
    const receitas = this.transacoes()
      .filter((tx) => tx.tipoMovimentacao === 'RECEITA')
      .reduce((sum, tx) => sum + Number(tx.valor), 0);

    const despesas = this.transacoes()
      .filter((tx) => tx.tipoMovimentacao === 'DESPESA')
      .reduce((sum, tx) => sum + Number(tx.valor), 0);

    return { receitas, despesas, saldo: receitas - despesas };
  });

  ngOnInit(): void {
    this.carregarCategorias();
    this.carregarRecorrentes();
    this.carregarPeriodos();
  }

  formatDate = formatDate;

  formatPeriodo(periodo: PeriodoFinanceiro): string {
    return `${formatDate(periodo.dataInicio)} até ${formatDate(periodo.dataFim)}`;
  }

  onPeriodoChange(rawPeriodoId: string): void {
    const periodoId = Number(rawPeriodoId);
    if (!Number.isFinite(periodoId)) {
      return;
    }

    this.selectedPeriodoId.set(periodoId);
    const periodoSelecionado = this.periodos().find((periodo) => periodo.id === periodoId);

    if (periodoSelecionado) {
      this.carregarResumoPorPeriodo(periodoSelecionado);
    }
  }

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
    this.reloadSelectedPeriodo();
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
          this.reloadSelectedPeriodo();
        },
        error: (err) => {
          this.deletingId.set(null);
          this.toast.show(mapApiError(err), 'error');
        },
      });
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

  private carregarPeriodos(): void {
    this.loadingPeriodos.set(true);
    this.errorMessage.set('');

    this.periodosApi
      .listAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          const periodos = page.content;
          this.periodos.set(periodos);
          this.loadingPeriodos.set(false);

          const periodoPadrao = this.findPeriodoAtual(periodos) ?? periodos[0] ?? null;

          if (!periodoPadrao) {
            this.loadingResumo.set(false);
            this.transacoes.set([]);
            return;
          }

          this.selectedPeriodoId.set(periodoPadrao.id);
          this.carregarResumoPorPeriodo(periodoPadrao);
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
          this.loadingPeriodos.set(false);
          this.loadingResumo.set(false);
        },
      });
  }

  private carregarResumoPorPeriodo(periodo: PeriodoFinanceiro): void {
    this.loadingResumo.set(true);
    this.errorMessage.set('');

    this.transacoesApi
      .listByPeriodo(periodo.id, periodo.dataInicio, periodo.dataFim)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.transacoes.set(page.content);
          this.loadingResumo.set(false);
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
          this.loadingResumo.set(false);
        },
      });
  }

  private carregarCategorias(): void {
    this.loadingCategorias.set(true);

    this.categoriasApi
      .listAll()
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

  private carregarRecorrentes(): void {
    this.loadingRecorrentes.set(true);

    this.transacoesRecorrentesApi
      .listAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.recorrentes.set(page.content);
          this.loadingRecorrentes.set(false);
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
          this.loadingRecorrentes.set(false);
        },
      });
  }

  private reloadSelectedPeriodo(): void {
    const periodo = this.selectedPeriodo();
    if (periodo) {
      this.carregarResumoPorPeriodo(periodo);
    }
  }

  private findPeriodoAtual(periodos: PeriodoFinanceiro[]): PeriodoFinanceiro | null {
    const nowIso = toIsoDate(new Date());
    return (
      periodos.find((periodo) => nowIso >= periodo.dataInicio && nowIso <= periodo.dataFim) ?? null
    );
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

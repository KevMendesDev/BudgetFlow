import { DOCUMENT } from '@angular/common';
import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Chart, ChartData, ChartOptions } from 'chart.js';
import ChartDataLabels from 'chartjs-plugin-datalabels';
import { NgChartsModule } from 'ng2-charts';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import {
  CategoriaResponse,
  CLASSIFICACAO_LABELS,
  CLASSIFICACOES,
  ClassificacaoCategoria,
} from '../../../../core/models/categoria.models';
import { NaturezaFinanceira } from '../../../../core/models/natureza-financeira.models';
import { PageSize } from '../../../../core/models/pagination.models';
import { PeriodoFinanceiro, PeriodoFinanceiroResponse } from '../../../../core/models/periodo-financeiro.models';
import { PlanejamentoResponse } from '../../../../core/models/planejamento.models';
import { TransacaoRecorrenteResponse } from '../../../../core/models/transacao-recorrente.models';
import { TIPOS_MOVIMENTACAO } from '../../../../core/models/transacao.models';
import { CategoriasApiService } from '../../../../core/services/categorias-api.service';
import { PeriodosApiService } from '../../../../core/services/periodos-api.service';
import { PlanejamentosApiService } from '../../../../core/services/planejamentos-api.service';
import { ThemeService } from '../../../../core/services/theme.service';
import { ToastService } from '../../../../core/services/toast.service';
import { TransacoesRecorrentesApiService } from '../../../../core/services/transacoes-recorrentes-api.service';
import { CurrencyBRLPipe } from '../../../../shared/pipes/currency-brl.pipe';
import { ConfirmDialogService } from '../../../../shared/services/confirm-dialog.service';
import { currencyTooltipLabel } from '../../../../shared/utils/chart-tooltip.util';
import { mapApiError } from '../../../../shared/utils/error-message.util';
import { formatMonthYear, toIsoDate } from '../../../../shared/utils/format.util';
import { isDesktopViewport } from '../../../../shared/utils/viewport.util';
import { PeriodoFinanceiroModalComponent } from '../../../periodos/components/periodo-financeiro-modal/periodo-financeiro-modal.component';
import { PlanejamentoModalComponent } from '../../components/planejamento-modal/planejamento-modal.component';

@Component({
  selector: 'app-planejamentos-page',
  imports: [
    CurrencyBRLPipe,
    NgChartsModule,
    PlanejamentoModalComponent,
    PeriodoFinanceiroModalComponent,
    ReactiveFormsModule,
  ],
  templateUrl: './planejamentos-page.component.html',
  styleUrl: './planejamentos-page.component.scss',
})
export class PlanejamentosPageComponent implements OnInit {
  static {
    Chart.register(ChartDataLabels);
  }

  private readonly formBuilder = inject(FormBuilder);
  private readonly categoriasApi = inject(CategoriasApiService);
  private readonly periodosApi = inject(PeriodosApiService);
  private readonly planejamentosApi = inject(PlanejamentosApiService);
  private readonly recorrentesApi = inject(TransacoesRecorrentesApiService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly theme = inject(ThemeService);
  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);

  readonly periodos = signal<PeriodoFinanceiro[]>([]);
  readonly categorias = signal<CategoriaResponse[]>([]);
  readonly planejamentos = signal<PlanejamentoResponse[]>([]);
  readonly listaPlanejamentos = signal<PlanejamentoResponse[]>([]);
  readonly resumoAberto = signal(false);
  readonly recorrentes = signal<TransacaoRecorrenteResponse[]>([]);
  readonly selectedPeriodoId = signal<number | null>(null);
  readonly loading = signal(true);
  readonly loadingPeriodos = signal(true);
  readonly syncing = signal(false);
  readonly deletingId = signal<number | null>(null);
  readonly deletingAll = signal(false);
  readonly errorMessage = signal('');
  readonly modalOpen = signal(false);
  readonly periodoModalOpen = signal(false);
  readonly editingPlanejamento = signal<PlanejamentoResponse | null>(null);
  readonly filtrosAbertos = signal(isDesktopViewport());
  readonly paginaAtual = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly classificacaoLabels = CLASSIFICACAO_LABELS;
  readonly classificacoes = CLASSIFICACOES;
  readonly tiposMovimentacao = TIPOS_MOVIMENTACAO;
  readonly chartType: 'doughnut' = 'doughnut';
  readonly skeletonCards = [1, 2, 3];

  readonly filtersForm = this.formBuilder.nonNullable.group({
    query: [''],
    tipoMovimentacao: ['' as '' | NaturezaFinanceira],
    classificacaoCategoria: ['' as '' | ClassificacaoCategoria],
  });

  readonly selectedPeriodo = computed(
    () => this.periodos().find((item) => item.id === this.selectedPeriodoId()) ?? null
  );
  readonly receitas = computed(() => this.sumByType('RECEITA'));
  readonly despesas = computed(() => this.sumByType('DESPESA'));
  readonly saldo = computed(() => this.receitas() - this.despesas());
  readonly despesasPlanejadas = computed(() =>
    this.planejamentos().filter((item) => item.tipoMovimentacao === 'DESPESA')
  );
  readonly categoriasDistinct = computed(() =>
    Array.from(new Set(this.planejamentos().map((item) => item.categoriaNome))).sort((a, b) =>
      a.localeCompare(b)
    )
  );

  readonly categoriaChartData = computed<ChartData<'doughnut'>>(() =>
    this.buildChartData(
      this.groupValues(this.despesasPlanejadas(), (item) => item.categoriaNome)
    )
  );
  readonly classificacaoChartData = computed<ChartData<'doughnut'>>(() =>
    this.buildChartData(
      this.groupValues(
        this.despesasPlanejadas().filter((item) => item.classificacaoCategoria),
        (item) => this.classificacaoLabels[item.classificacaoCategoria!]
      )
    )
  );
  readonly chartOptions = computed<ChartOptions<'doughnut'>>(() => {
    this.theme.effectiveTheme();
    const receitas = this.receitas();
    return {
      responsive: true,
      maintainAspectRatio: false,
      cutout: '62%',
      plugins: {
        legend: { position: 'bottom', labels: { color: this.cssVar('--muted-text') } },
        tooltip: {
          enabled: true,
          callbacks: { label: currencyTooltipLabel },
        },
        datalabels: {
          color: this.cssVar('--text'),
          font: { weight: 600 },
          formatter: (value) => {
            const percentual = receitas > 0 ? (Number(value) / receitas) * 100 : 0;
            return percentual >= 1 ? `${Math.round(percentual)}%` : '';
          },
        },
      },
    };
  });

  ngOnInit(): void {
    this.loadCategorias();
    this.loadRecorrentes();
    this.loadPeriodos();

    this.filtersForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => this.loadLista(0));
  }

  onPeriodoChange(value: string): void {
    const id = Number(value);
    if (Number.isFinite(id)) {
      this.selectedPeriodoId.set(id);
      this.reload();
    }
  }

  openPeriodoModal(): void {
    this.periodoModalOpen.set(true);
  }

  closePeriodoModal(): void {
    this.periodoModalOpen.set(false);
  }

  onPeriodoSaved(periodo: PeriodoFinanceiroResponse): void {
    this.closePeriodoModal();
    this.loadingPeriodos.set(true);
    this.errorMessage.set('');

    this.periodosApi
      .listAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.periodos.set(page.content);
          this.loadingPeriodos.set(false);
          this.selectedPeriodoId.set(periodo.id);
          this.reload();
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
          this.loadingPeriodos.set(false);
        },
      });
  }

  toggleResumo(): void {
    this.resumoAberto.update((aberto) => !aberto);
  }

  toggleFiltros(): void {
    this.filtrosAbertos.update((aberto) => !aberto);
  }

  clearFilters(): void {
    this.filtersForm.setValue(
      { query: '', tipoMovimentacao: '', classificacaoCategoria: '' },
      { emitEvent: false }
    );
    this.loadLista(0);
  }

  openCreateModal(): void {
    if (!this.selectedPeriodo()) {
      return;
    }
    this.loadRecorrentes();
    this.editingPlanejamento.set(null);
    this.modalOpen.set(true);
  }

  startEdit(item: PlanejamentoResponse): void {
    this.editingPlanejamento.set(item);
    this.modalOpen.set(true);
  }

  closeModal(): void {
    this.modalOpen.set(false);
  }

  onSaved(): void {
    this.modalOpen.set(false);
    this.reload();
  }

  onCategoriasChanged(): void {
    this.loadCategorias();
  }

  sincronizar(): void {
    const periodo = this.selectedPeriodo();
    if (!periodo || this.syncing()) {
      return;
    }
    this.syncing.set(true);
    this.planejamentosApi
      .sincronizarRecorrentes(periodo.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.toast.show(response.mensagem, 'success');
          this.syncing.set(false);
          this.reload();
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
          this.syncing.set(false);
        },
      });
  }

  async delete(item: PlanejamentoResponse): Promise<void> {
    const confirmed = await this.confirmDialog.confirm(`Excluir o planejamento "${item.descricao}"?`);
    if (!confirmed) {
      return;
    }
    this.deletingId.set(item.id);
    this.planejamentosApi
      .delete(item.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.deletingId.set(null);
          this.toast.show('Planejamento excluído.', 'success');
          this.reload();
        },
        error: () => this.deletingId.set(null),
      });
  }

  async deleteAll(): Promise<void> {
    const periodo = this.selectedPeriodo();
    if (!periodo || this.deletingAll()) {
      return;
    }

    const confirmed = await this.confirmDialog.confirm(
      'Essa alteração não pode ser desfeita. Deseja realmente excluir todos os planejamentos deste período?'
    );
    if (!confirmed) {
      return;
    }

    this.deletingAll.set(true);
    this.planejamentosApi
      .deleteAllByPeriodo(periodo.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.deletingAll.set(false);
          this.toast.show('Todos os planejamentos do período foram excluídos.', 'success');
          this.reload();
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
          this.deletingAll.set(false);
        },
      });
  }

  goToPreviousPage(): void {
    const atual = this.paginaAtual();
    if (atual > 0) {
      this.loadLista(atual - 1);
    }
  }

  goToNextPage(): void {
    const atual = this.paginaAtual();
    if (atual + 1 < this.totalPages()) {
      this.loadLista(atual + 1);
    }
  }

  formatPeriodo(periodo: PeriodoFinanceiro): string {
    return formatMonthYear(periodo.mes, periodo.ano);
  }

  private loadPeriodos(): void {
    this.loading.set(true);
    this.loadingPeriodos.set(true);
    this.periodosApi
      .listAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.periodos.set(response.content);
          this.loadingPeriodos.set(false);
          const periodo = this.findPeriodoAtual(response.content) ?? response.content[0] ?? null;
          this.selectedPeriodoId.set(periodo?.id ?? null);
          if (periodo) {
            this.reload();
          } else {
            this.planejamentos.set([]);
            this.listaPlanejamentos.set([]);
            this.loading.set(false);
          }
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
          this.loadingPeriodos.set(false);
          this.loading.set(false);
        },
      });
  }

  private loadCategorias(): void {
    this.categoriasApi
      .listAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => this.categorias.set(response.content),
        error: (err) => this.errorMessage.set(mapApiError(err)),
      });
  }

  private loadRecorrentes(): void {
    this.recorrentesApi
      .listAll({ status: 'ATIVA' })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => this.recorrentes.set(response.content),
        error: (err) => this.errorMessage.set(mapApiError(err)),
      });
  }

  private reload(): void {
    const id = this.selectedPeriodoId();
    if (!id) {
      return;
    }
    this.loadResumo(id);
    this.loadLista(0);
  }

  private loadResumo(periodoId: number): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.planejamentosApi
      .listByPeriodo(periodoId, PageSize.BULK)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.planejamentos.set(page.content);
          this.loading.set(false);
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
          this.loading.set(false);
        },
      });
  }

  private loadLista(page = this.paginaAtual()): void {
    const periodoId = this.selectedPeriodoId();
    if (!periodoId) {
      this.listaPlanejamentos.set([]);
      this.paginaAtual.set(0);
      this.totalPages.set(0);
      this.totalElements.set(0);
      return;
    }

    const filters = this.filtersForm.getRawValue();
    this.planejamentosApi
      .listFiltered({
        periodoId,
        page,
        size: PageSize.DEFAULT,
        query: filters.query,
        tipoMovimentacao: filters.tipoMovimentacao,
        classificacaoCategoria: filters.classificacaoCategoria,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          const totalPages = response.page?.totalPages ?? response.totalPages ?? 0;
          const totalElements = response.page?.totalElements ?? response.totalElements ?? 0;

          if (response.content.length === 0 && totalPages > 0 && this.paginaAtual() >= totalPages) {
            this.loadLista(totalPages - 1);
            return;
          }

          this.listaPlanejamentos.set(response.content);
          this.paginaAtual.set(response.page?.number ?? response.number ?? 0);
          this.totalPages.set(totalPages);
          this.totalElements.set(totalElements);
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
          this.listaPlanejamentos.set([]);
          this.totalPages.set(0);
          this.totalElements.set(0);
        },
      });
  }

  private sumByType(tipo: 'RECEITA' | 'DESPESA'): number {
    return this.planejamentos()
      .filter((item) => item.tipoMovimentacao === tipo)
      .reduce((total, item) => total + Number(item.valor), 0);
  }

  private groupValues(
    items: PlanejamentoResponse[],
    key: (item: PlanejamentoResponse) => string
  ): Map<string, number> {
    const values = new Map<string, number>();
    items.forEach((item) => values.set(key(item), (values.get(key(item)) ?? 0) + Number(item.valor)));
    return values;
  }

  private buildChartData(values: Map<string, number>): ChartData<'doughnut'> {
    const entries = Array.from(values.entries()).sort((a, b) => b[1] - a[1]);
    return {
      labels: entries.map(([label]) => label),
      datasets: [{
        data: entries.map(([, value]) => value),
        backgroundColor: entries.map((_, index) => `hsl(${(index * 137.508) % 360}, 68%, 52%)`),
        borderWidth: 0,
      }],
    };
  }

  private findPeriodoAtual(periodos: PeriodoFinanceiro[]): PeriodoFinanceiro | null {
    const now = new Date();
    const mes = now.getMonth() + 1;
    const ano = now.getFullYear();
    const porMesAno = periodos.find((item) => item.mes === mes && item.ano === ano);
    if (porMesAno) {
      return porMesAno;
    }

    const nowIso = toIsoDate(now);
    return (
      periodos.find((item) => nowIso >= item.dataInicio && nowIso <= item.dataFim) ?? null
    );
  }

  private cssVar(name: string): string {
    return getComputedStyle(this.document.documentElement).getPropertyValue(name).trim();
  }
}

import { DOCUMENT } from '@angular/common';
import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Chart, ChartData, ChartOptions } from 'chart.js';
import ChartDataLabels from 'chartjs-plugin-datalabels';
import { NgChartsModule } from 'ng2-charts';

import {
  CategoriaResponse,
  CLASSIFICACAO_LABELS,
} from '../../../../core/models/categoria.models';
import { PeriodoFinanceiro } from '../../../../core/models/periodo-financeiro.models';
import { PlanejamentoResponse } from '../../../../core/models/planejamento.models';
import { TransacaoRecorrenteResponse } from '../../../../core/models/transacao-recorrente.models';
import { CategoriasApiService } from '../../../../core/services/categorias-api.service';
import { PeriodosApiService } from '../../../../core/services/periodos-api.service';
import { PlanejamentosApiService } from '../../../../core/services/planejamentos-api.service';
import { TransacoesRecorrentesApiService } from '../../../../core/services/transacoes-recorrentes-api.service';
import { ThemeService } from '../../../../core/services/theme.service';
import { ToastService } from '../../../../core/services/toast.service';
import { CurrencyBRLPipe } from '../../../../shared/pipes/currency-brl.pipe';
import { ConfirmDialogService } from '../../../../shared/services/confirm-dialog.service';
import { mapApiError } from '../../../../shared/utils/error-message.util';
import { formatMonthYear, toIsoDate } from '../../../../shared/utils/format.util';
import { PlanejamentoModalComponent } from '../../components/planejamento-modal/planejamento-modal.component';

@Component({
  selector: 'app-planejamentos-page',
  imports: [CurrencyBRLPipe, NgChartsModule, PlanejamentoModalComponent],
  templateUrl: './planejamentos-page.component.html',
  styleUrl: './planejamentos-page.component.scss',
})
export class PlanejamentosPageComponent implements OnInit {
  static {
    Chart.register(ChartDataLabels);
  }

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
  readonly recorrentes = signal<TransacaoRecorrenteResponse[]>([]);
  readonly selectedPeriodoId = signal<number | null>(null);
  readonly loading = signal(true);
  readonly syncing = signal(false);
  readonly deletingId = signal<number | null>(null);
  readonly errorMessage = signal('');
  readonly modalOpen = signal(false);
  readonly editingPlanejamento = signal<PlanejamentoResponse | null>(null);
  readonly classificacaoLabels = CLASSIFICACAO_LABELS;
  readonly chartType: 'doughnut' = 'doughnut';
  readonly skeletonCards = [1, 2, 3];

  readonly selectedPeriodo = computed(
    () => this.periodos().find((item) => item.id === this.selectedPeriodoId()) ?? null
  );
  readonly receitas = computed(() => this.sumByType('RECEITA'));
  readonly despesas = computed(() => this.sumByType('DESPESA'));
  readonly saldo = computed(() => this.receitas() - this.despesas());
  readonly despesasPlanejadas = computed(() =>
    this.planejamentos().filter((item) => item.tipoMovimentacao === 'DESPESA')
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
  }

  onPeriodoChange(value: string): void {
    const id = Number(value);
    if (Number.isFinite(id)) {
      this.selectedPeriodoId.set(id);
      this.loadPlanejamentos(id);
    }
  }

  openCreateModal(): void {
    if (!this.selectedPeriodo()) {
      return;
    }
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

  formatPeriodo(periodo: PeriodoFinanceiro): string {
    return formatMonthYear(periodo.mes, periodo.ano);
  }

  private loadPeriodos(): void {
    this.loading.set(true);
    this.periodosApi
      .listAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.periodos.set(response.content);
          const periodo = this.findPeriodoAtual(response.content) ?? response.content[0] ?? null;
          this.selectedPeriodoId.set(periodo?.id ?? null);
          if (periodo) {
            this.loadPlanejamentos(periodo.id);
          } else {
            this.loading.set(false);
          }
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
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
      .listAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => this.recorrentes.set(response.content),
        error: (err) => this.errorMessage.set(mapApiError(err)),
      });
  }

  private loadPlanejamentos(periodoId: number): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.planejamentosApi
      .listByPeriodo(periodoId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (items) => {
          this.planejamentos.set(items);
          this.loading.set(false);
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
          this.loading.set(false);
        },
      });
  }

  private reload(): void {
    const id = this.selectedPeriodoId();
    if (id) {
      this.loadPlanejamentos(id);
    }
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
    const hoje = toIsoDate(new Date());
    return periodos.find((item) => hoje >= item.dataInicio && hoje <= item.dataFim) ?? null;
  }

  private cssVar(name: string): string {
    return getComputedStyle(this.document.documentElement).getPropertyValue(name).trim();
  }
}

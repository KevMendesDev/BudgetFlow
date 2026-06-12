import { DOCUMENT } from '@angular/common';
import { Component, computed, inject, input, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Chart, ChartData, ChartOptions } from 'chart.js';
import { NgChartsModule } from 'ng2-charts';
import ChartDataLabels from 'chartjs-plugin-datalabels';

import {
  CategoriaResponse,
  ClassificacaoCategoria,
  CLASSIFICACAO_LABELS,
} from '../../../../core/models/categoria.models';
import { PeriodoFinanceiro } from '../../../../core/models/periodo-financeiro.models';
import { TransacaoResponse } from '../../../../core/models/transacao.models';
import { CurrencyBRLPipe } from '../../../../shared/pipes/currency-brl.pipe';
import { formatDate } from '../../../../shared/utils/format.util';
import { ThemeService } from '../../../../core/services/theme.service';
import {
  buildCategoriaChartData,
  buildCategoriasLegenda,
  buildClassificacaoChartData,
  buildColorMap,
  buildDailyChartData,
  buildResumoCategorias,
  buildResumoClassificacao,
  CategoriaResumo,
  ClassificacaoResumo,
  filterDespesas,
  sumValores,
} from '../../utils/dashboard-resumo.util';

@Component({
  selector: 'app-dashboard-resumo-categorias',
  imports: [CurrencyBRLPipe, DecimalPipe, NgChartsModule],
  templateUrl: './dashboard-resumo-categorias.component.html',
  styleUrl: './dashboard-resumo-categorias.component.scss',
})
export class DashboardResumoCategoriasComponent {
  static {
    Chart.register(ChartDataLabels);
  }
  private readonly document = inject(DOCUMENT);
  private readonly theme = inject(ThemeService);

  readonly categorias = input.required<CategoriaResponse[]>();
  readonly transacoes = input.required<TransacaoResponse[]>();
  readonly selectedPeriodo = input<PeriodoFinanceiro | null>(null);

  readonly aberto = signal(false);
  readonly classificacaoLabels = CLASSIFICACAO_LABELS;
  readonly barChartType: 'bar' = 'bar';
  readonly doughnutChartType: 'doughnut' = 'doughnut';
  readonly categoriaChartType: 'doughnut' = 'doughnut';

  readonly despesas = computed(() => filterDespesas(this.transacoes()));

  readonly saldo = computed(() => this.totalReceitas() - sumValores(this.despesas()));

  readonly totalReceitas = computed(() => sumValores(this.transacoes().filter((tx) => tx.tipoMovimentacao === 'RECEITA')));

  readonly resumoCategorias = computed<CategoriaResumo[]>(() =>
    buildResumoCategorias(this.despesas(), this.saldo(), this.totalReceitas(), (key) => this.colorForKey(key))
  );

  readonly dailyChartOptions = computed<ChartOptions<'bar'>>(() => {
    this.theme.effectiveTheme();

    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: { enabled: true },
        datalabels: { display: false },
      },
      scales: {
        x: {
          stacked: true,
          ticks: { color: this.cssVar('--muted-text'), maxRotation: 0, autoSkip: true },
          grid: { color: this.cssVar('--chart-grid') },
        },
        y: {
          stacked: true,
          ticks: { color: this.cssVar('--muted-text') },
          grid: { color: this.cssVar('--chart-grid') },
        },
      },
    };
  });

  readonly classificacaoChartOptions = computed<ChartOptions<'doughnut'>>(() => {
    this.theme.effectiveTheme();
    const lucroBase = Math.max(1, this.totalReceitas());

    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'bottom', labels: { color: this.cssVar('--muted-text') } },
        datalabels: {
          color: this.cssVar('--text'),
          font: { weight: 600 },
          formatter: (value) => {
            const percent = (Number(value) / lucroBase) * 100;
            return percent >= 1 ? `${Math.round(percent)}%` : '';
          },
        },
      },
      cutout: '68%',
    };
  });

  readonly categoriaChartOptions = computed<ChartOptions<'doughnut'>>(() => {
    this.theme.effectiveTheme();

    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: { enabled: false },
        datalabels: { display: false },
      },
      cutout: '70%',
    };
  });

  readonly categoriasLegenda = computed(() => {
    return buildCategoriasLegenda(this.despesas(), (key) => this.colorForKey(key));
  });

  readonly colorMap = computed(() => {
    const keys = this.transacoes().flatMap((tx) => [
      ...(tx.categoriaNome ? [tx.categoriaNome] : []),
      ...(tx.classificacaoCategoria ? [tx.classificacaoCategoria] : []),
    ]);

    return buildColorMap(keys, (index) => this.colorFromIndex(index));
  });

  readonly dailyChartData = computed<ChartData<'bar'>>(() => {
    return buildDailyChartData(this.selectedPeriodo(), this.despesas(), (key) => this.colorForKey(key));
  });

  readonly classificacaoChartData = computed<ChartData<'doughnut'>>(() => {
    return buildClassificacaoChartData(
      this.resumoClassificacao(),
      (classificacao) => this.classificacaoLabels[classificacao],
      (key) => this.colorForKey(key)
    );
  });

  readonly resumoClassificacao = computed<ClassificacaoResumo[]>(() => buildResumoClassificacao(this.despesas()));

  toggle(): void {
    this.aberto.update((value) => !value);
  }

  categoriaChartData(item: CategoriaResumo): ChartData<'doughnut'> {
    this.theme.effectiveTheme();
    return buildCategoriaChartData(item.total, this.totalReceitas(), item.color, this.cssVar('--chart-track'));
  }

  formatDate = formatDate;

  private colorForKey(key: string): string {
    return this.colorMap().get(key) ?? this.colorFromIndex(this.hashToIndex(key));
  }

  private colorFromIndex(index: number): string {
    const hue = (index * 137.508) % 360;
    const saturation = index % 2 === 0 ? 70 : 60;
    const lightness = index % 3 === 0 ? 55 : 50;
    return `hsl(${hue.toFixed(0)}, ${saturation}%, ${lightness}%)`;
  }

  private hashToIndex(key: string): number {
    let hash = 0;
    for (let i = 0; i < key.length; i += 1) {
      hash = (hash << 5) - hash + key.charCodeAt(i);
      hash |= 0;
    }
    return Math.abs(hash);
  }

  private cssVar(name: string): string {
    return getComputedStyle(this.document.documentElement).getPropertyValue(name).trim();
  }
}

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
import { formatDate, toIsoDate } from '../../../../shared/utils/format.util';
import { ThemeService } from '../../../../core/services/theme.service';

interface CategoriaResumo {
  nome: string;
  classificacao: ClassificacaoCategoria;
  total: number;
  percentualSaldo: number;
  percentualLucro: number;
  color: string;
}

interface ClassificacaoResumo {
  classificacao: ClassificacaoCategoria;
  total: number;
  widthPercent: number;
}

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

  readonly saldo = computed(() => {
    const receitas = this.totalReceitas();
    const despesas = this.sumValores(this.getDespesas());
    return receitas - despesas;
  });

  readonly totalReceitas = computed(() =>
    this.sumValores(
      this.transacoes().filter((tx) => tx.tipoMovimentacao === 'RECEITA')
    )
  );

  readonly resumoCategorias = computed<CategoriaResumo[]>(() => {
    const saldoBase = Math.max(1, Math.abs(this.saldo()));
    const lucroBase = Math.max(1, this.totalReceitas());
    const mapa = new Map<string, CategoriaResumo>();

    this.getDespesas().forEach((tx) => {
        const key = `${tx.categoriaNome}__${tx.classificacaoCategoria}`;
        const atual = mapa.get(key);
        const total = (atual?.total ?? 0) + Number(tx.valor);
        mapa.set(key, {
          nome: tx.categoriaNome,
          classificacao: tx.classificacaoCategoria as ClassificacaoCategoria,
          total,
          percentualSaldo: Math.min(100, (total / saldoBase) * 100),
          percentualLucro: Math.min(100, (total / lucroBase) * 100),
          color: this.colorForKey(tx.categoriaNome),
        });
      });

    return Array.from(mapa.values()).sort((a, b) => b.total - a.total);
  });

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
    const nomes = new Set<string>();
    this.getDespesas().forEach((tx) => nomes.add(tx.categoriaNome));

    return Array.from(nomes).map((nome) => ({
      nome,
      color: this.colorForKey(nome),
    }));
  });

  readonly colorMap = computed(() => {
    const keys = new Set<string>();
    this.transacoes().forEach((tx) => {
      if (tx.categoriaNome) {
        keys.add(tx.categoriaNome);
      }
      if (tx.classificacaoCategoria) {
        keys.add(tx.classificacaoCategoria);
      }
    });

    const list = Array.from(keys).sort((a, b) => a.localeCompare(b));
    const map = new Map<string, string>();
    list.forEach((key, index) => {
      map.set(key, this.colorFromIndex(index));
    });
    return map;
  });

  readonly dailyChartData = computed<ChartData<'bar'>>(() => {
    const periodo = this.selectedPeriodo();
    if (!periodo) {
      return { labels: [], datasets: [] };
    }

    const dias = this.getDiasNoPeriodo(periodo.dataInicio, periodo.dataFim);
    const labels = dias.map((dia) => formatDate(dia));
    const categorias = this.getCategoriasUnicas(this.getDespesas());

    const datasets = categorias.map((categoria) => {
      const data = dias.map((dia) =>
        this.sumValores(
          this.getDespesas().filter((tx) => tx.data === dia && tx.categoriaNome === categoria)
        )
      );

      return {
        label: categoria,
        data,
        backgroundColor: this.colorForKey(categoria),
        borderRadius: 6,
        borderSkipped: false as const,
      };
    });

    return { labels, datasets };
  });

  readonly classificacaoChartData = computed<ChartData<'doughnut'>>(() => {
    const classificacoes = this.resumoClassificacao();
    return {
      labels: classificacoes.map((item) => this.classificacaoLabels[item.classificacao]),
      datasets: [
        {
          data: classificacoes.map((item) => item.total),
          backgroundColor: classificacoes.map((item) => this.colorForKey(item.classificacao)),
          borderWidth: 0,
        },
      ],
    };
  });

  readonly resumoClassificacao = computed<ClassificacaoResumo[]>(() => {
    const mapa = new Map<ClassificacaoCategoria, number>();

    this.getDespesas().forEach((tx) => {
        const classificacao = tx.classificacaoCategoria as ClassificacaoCategoria;
        mapa.set(classificacao, (mapa.get(classificacao) ?? 0) + Number(tx.valor));
      });

    const totais = Array.from(mapa.entries()).map(([classificacao, total]) => ({
      classificacao,
      total,
    }));

    const maxTotal = Math.max(1, ...totais.map((item) => item.total));

    return totais
      .map((item) => ({
        ...item,
        widthPercent: (item.total / maxTotal) * 100,
      }))
      .sort((a, b) => b.total - a.total);
  });

  toggle(): void {
    this.aberto.update((value) => !value);
  }

  categoriaChartData(item: CategoriaResumo): ChartData<'doughnut'> {
    this.theme.effectiveTheme();
    const receitas = Math.max(1, this.totalReceitas());
    const restante = Math.max(0, receitas - item.total);
    return {
      labels: ['Gasto', 'Restante'],
      datasets: [
        {
          data: [item.total, restante],
          backgroundColor: [item.color, this.cssVar('--chart-track')],
          borderWidth: 0,
        },
      ],
    };
  }

  formatDate = formatDate;

  private getDespesas(): TransacaoResponse[] {
    return this.transacoes().filter((tx) => tx.tipoMovimentacao === 'DESPESA');
  }

  private getCategoriasUnicas(transacoes: TransacaoResponse[]): string[] {
    return Array.from(new Set(transacoes.map((tx) => tx.categoriaNome)));
  }

  private sumValores(transacoes: TransacaoResponse[]): number {
    return transacoes.reduce((sum, tx) => sum + Number(tx.valor), 0);
  }

  private getDiasNoPeriodo(inicio: string, fim: string): string[] {
    const dias: string[] = [];
    let atual = new Date(`${inicio}T00:00:00`);
    const end = new Date(`${fim}T00:00:00`);

    while (atual <= end) {
      dias.push(toIsoDate(atual));
      atual.setDate(atual.getDate() + 1);
    }

    return dias;
  }

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

import { DOCUMENT } from '@angular/common';
import { Component, computed, inject, input, signal } from '@angular/core';
import { ChartOptions } from 'chart.js';
import { NgChartsModule } from 'ng2-charts';

import { PlanejamentoResponse } from '../../../../core/models/planejamento.models';
import { TransacaoResponse } from '../../../../core/models/transacao.models';
import { ThemeService } from '../../../../core/services/theme.service';
import {
  buildCategoriaComparativoChartData,
  buildComparativoPlanejamento,
  buildTotaisComparativoChartData,
} from '../../utils/dashboard-planejamento.util';

@Component({
  selector: 'app-dashboard-planejamento-comparativo',
  imports: [NgChartsModule],
  templateUrl: './dashboard-planejamento-comparativo.component.html',
  styleUrl: './dashboard-planejamento-comparativo.component.scss',
})
export class DashboardPlanejamentoComparativoComponent {
  private readonly document = inject(DOCUMENT);
  private readonly theme = inject(ThemeService);

  readonly planejamentos = input.required<PlanejamentoResponse[]>();
  readonly transacoes = input.required<TransacaoResponse[]>();
  readonly chartType: 'bar' = 'bar';
  readonly aberto = signal(false);

  readonly comparativo = computed(() =>
    buildComparativoPlanejamento(this.planejamentos(), this.transacoes())
  );
  readonly categoriaChartData = computed(() =>
    buildCategoriaComparativoChartData(this.comparativo())
  );
  readonly totaisChartData = computed(() =>
    buildTotaisComparativoChartData(this.comparativo())
  );
  readonly chartOptions = computed<ChartOptions<'bar'>>(() => {
    this.theme.effectiveTheme();
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'bottom', labels: { color: this.cssVar('--muted-text') } },
        datalabels: { display: false },
      },
      scales: {
        x: {
          ticks: { color: this.cssVar('--muted-text') },
          grid: { color: this.cssVar('--chart-grid') },
        },
        y: {
          beginAtZero: true,
          ticks: { color: this.cssVar('--muted-text') },
          grid: { color: this.cssVar('--chart-grid') },
        },
      },
    };
  });

  toggle(): void {
    this.aberto.update((value) => !value);
  }

  private cssVar(name: string): string {
    return getComputedStyle(this.document.documentElement).getPropertyValue(name).trim();
  }
}

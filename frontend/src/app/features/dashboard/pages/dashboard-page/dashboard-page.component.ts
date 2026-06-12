import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { CategoriaResponse } from '../../../../core/models/categoria.models';
import { PeriodoFinanceiro } from '../../../../core/models/periodo-financeiro.models';
import { TransacaoRecorrenteResponse } from '../../../../core/models/transacao-recorrente.models';
import { SessionService } from '../../../../core/services/session.service';
import { TransacaoResponse } from '../../../../core/models/transacao.models';
import { CategoriasApiService } from '../../../../core/services/categorias-api.service';
import { PeriodosApiService } from '../../../../core/services/periodos-api.service';
import { TransacoesApiService } from '../../../../core/services/transacoes-api.service';
import { TransacoesRecorrentesApiService } from '../../../../core/services/transacoes-recorrentes-api.service';
import { CurrencyBRLPipe } from '../../../../shared/pipes/currency-brl.pipe';
import { mapApiError } from '../../../../shared/utils/error-message.util';
import { formatMonthYear, toIsoDate } from '../../../../shared/utils/format.util';
import { ToastService } from '../../../../core/services/toast.service';
import { DashboardTransacoesComponent } from '../../components/dashboard-transacoes/dashboard-transacoes.component';
import { DashboardResumoCategoriasComponent } from '../../components/dashboard-resumo-categorias/dashboard-resumo-categorias.component';

@Component({
  selector: 'app-dashboard-page',
  imports: [CurrencyBRLPipe, DashboardResumoCategoriasComponent, DashboardTransacoesComponent],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
})
export class DashboardPageComponent implements OnInit {
  private readonly categoriasApi = inject(CategoriasApiService);
  private readonly periodosApi = inject(PeriodosApiService);
  private readonly session = inject(SessionService);
  private readonly transacoesApi = inject(TransacoesApiService);
  private readonly transacoesRecorrentesApi = inject(TransacoesRecorrentesApiService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly user = computed(() => this.session.user());
  readonly loadingPeriodos = signal(true);
  readonly loadingResumo = signal(true);
  readonly loadingCategorias = signal(true);
  readonly loading = computed(() => this.loadingPeriodos() || this.loadingResumo());
  readonly errorMessage = signal('');
  readonly periodos = signal<PeriodoFinanceiro[]>([]);
  readonly categorias = signal<CategoriaResponse[]>([]);
  readonly recorrentes = signal<TransacaoRecorrenteResponse[]>([]);
  readonly selectedPeriodoId = signal<number | null>(null);
  readonly transacoes = signal<TransacaoResponse[]>([]);

  readonly skeletonCards = [1, 2, 3];

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
    this.loadCategorias();
    this.loadRecorrentes();
    this.loadPeriodos();
  }

  formatPeriodo(periodo: PeriodoFinanceiro): string {
    return formatMonthYear(periodo.mes, periodo.ano);
  }

  onPeriodoChange(rawPeriodoId: string): void {
    const periodoId = Number(rawPeriodoId);
    if (!Number.isFinite(periodoId)) {
      return;
    }

    this.selectedPeriodoId.set(periodoId);
    const periodoSelecionado = this.periodos().find((periodo) => periodo.id === periodoId);

    if (periodoSelecionado) {
      this.loadResumoPorPeriodo(periodoSelecionado);
    }
  }

  onTransacoesChanged(): void {
    this.reloadSelectedPeriodo();
  }

  onSincronizarRecorrentes(): void {
    const periodo = this.selectedPeriodo();
    if (!periodo) {
      return;
    }

    this.transacoesApi
      .sincronizarRecorrentes(periodo.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.toast.show(response.mensagem, 'success');
          this.reloadSelectedPeriodo();
          this.loadRecorrentes();
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
        },
      });
  }

  private loadPeriodos(): void {
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
          this.loadResumoPorPeriodo(periodoPadrao);
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
          this.loadingPeriodos.set(false);
          this.loadingResumo.set(false);
        },
      });
  }

  private loadResumoPorPeriodo(periodo: PeriodoFinanceiro): void {
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

  private loadCategorias(): void {
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

  private loadRecorrentes(): void {
    this.transacoesRecorrentesApi
      .listAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.recorrentes.set(page.content);
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
        },
      });
  }

  private reloadSelectedPeriodo(): void {
    const periodo = this.selectedPeriodo();
    if (periodo) {
      this.loadResumoPorPeriodo(periodo);
    }
  }

  private findPeriodoAtual(periodos: PeriodoFinanceiro[]): PeriodoFinanceiro | null {
    const nowIso = toIsoDate(new Date());
    return (
      periodos.find((periodo) => nowIso >= periodo.dataInicio && nowIso <= periodo.dataFim) ?? null
    );
  }

}

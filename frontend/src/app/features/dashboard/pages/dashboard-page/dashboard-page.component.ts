import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CategoriaResponse } from '../../../../core/models/categoria.models';
import { PeriodoFinanceiro } from '../../../../core/models/periodo.models';
import { TransacaoRecorrenteResponse } from '../../../../core/models/transacao-recorrente.models';
import { SessionService } from '../../../../core/services/session.service';
import { TipoMovimentacao, TipoPagamento, TransacaoResponse } from '../../../../core/models/transacao.models';
import { CategoriasApiService } from '../../../../core/services/categorias-api.service';
import { PeriodosApiService } from '../../../../core/services/periodos-api.service';
import { TransacoesApiService } from '../../../../core/services/transacoes-api.service';
import { TransacoesRecorrentesApiService } from '../../../../core/services/transacoes-recorrentes-api.service';
import { ToastService } from '../../../../core/services/toast.service';
import { mapApiError } from '../../../../shared/utils/error-message.util';

@Component({
  selector: 'app-dashboard-page',
  imports: [ReactiveFormsModule],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
})
export class DashboardPageComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly categoriasApi = inject(CategoriasApiService);
  private readonly periodosApi = inject(PeriodosApiService);
  private readonly session = inject(SessionService);
  private readonly toast = inject(ToastService);
  private readonly transacoesApi = inject(TransacoesApiService);
  private readonly transacoesRecorrentesApi = inject(TransacoesRecorrentesApiService);

  readonly user = computed(() => this.session.user());
  readonly loadingPeriodos = signal(true);
  readonly loadingResumo = signal(true);
  readonly loadingRecorrentes = signal(true);
  readonly loadingCategorias = signal(true);
  readonly loading = computed(() => this.loadingPeriodos() || this.loadingResumo());
  readonly submitting = signal(false);
  readonly deletingId = signal<number | null>(null);
  readonly modalOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly errorMessage = signal('');
  readonly modalErrorMessage = signal('');
  readonly periodos = signal<PeriodoFinanceiro[]>([]);
  readonly categorias = signal<CategoriaResponse[]>([]);
  readonly recorrentes = signal<TransacaoRecorrenteResponse[]>([]);
  readonly selectedPeriodoId = signal<number | null>(null);
  readonly transacoes = signal<TransacaoResponse[]>([]);
  readonly skeletonCards = [1, 2, 3];
  readonly skeletonRows = [1, 2, 3, 4, 5, 6];
  readonly tiposMovimentacao: Array<{ value: TipoMovimentacao; label: string }> = [
    { value: 'RECEITA', label: 'Receita' },
    { value: 'DESPESA', label: 'Despesa' },
  ];
  readonly tiposPagamento: Array<{ value: TipoPagamento; label: string }> = [
    { value: 'DINHEIRO', label: 'Dinheiro' },
    { value: 'CARTAO_CREDITO', label: 'Cartao de credito' },
    { value: 'CARTAO_DEBITO', label: 'Cartao de debito' },
    { value: 'PIX', label: 'Pix' },
    { value: 'TRANSFERENCIA', label: 'Transferencia' },
    { value: 'BOLETO', label: 'Boleto' },
  ];

  readonly form = this.formBuilder.nonNullable.group({
    transacaoRecorrenteId: [''],
    categoriaId: [''],
    descricao: ['', [Validators.maxLength(255)]],
    valor: ['', [Validators.min(0.01)]],
    tipoMovimentacao: ['' as '' | TipoMovimentacao],
    tipoPagamento: ['' as '' | TipoPagamento],
    data: ['', [Validators.required]],
  });

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

    return {
      receitas,
      despesas,
      saldo: receitas - despesas,
    };
  });

  readonly transacoesRecentes = computed(() => this.transacoes());

  readonly recorrentesDisponiveis = computed(() => {
    const selectedRecorrenteId = Number(this.form.controls.transacaoRecorrenteId.value || 0);
    const idsUsados = new Set(
      this.transacoes()
        .filter((tx) => tx.transacaoRecorrenteId)
        .map((tx) => tx.transacaoRecorrenteId as number)
    );

    return this.recorrentes().filter((recorrente) => {
      if (selectedRecorrenteId === recorrente.id) {
        return true;
      }
      return !idsUsados.has(recorrente.id);
    });
  });

  ngOnInit(): void {
    this.carregarCategorias();
    this.carregarRecorrentes();
    this.carregarPeriodos();
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      minimumFractionDigits: 2,
    }).format(value);
  }

  formatDate(value: string): string {
    return new Date(`${value}T00:00:00`).toLocaleDateString('pt-BR');
  }

  formatPeriodo(periodo: PeriodoFinanceiro): string {
    return `${this.formatDate(periodo.dataInicio)} ate ${this.formatDate(periodo.dataFim)}`;
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
    this.modalOpen.set(true);
    this.resetForm();

    const periodo = this.selectedPeriodo();
    if (periodo) {
      this.form.controls.data.setValue(this.clampDateToPeriodo(this.toIsoDate(new Date()), periodo));
    }
  }

  startEdit(tx: TransacaoResponse): void {
    this.modalOpen.set(true);
    this.editingId.set(tx.id);
    this.form.setValue({
      transacaoRecorrenteId: tx.transacaoRecorrenteId ? String(tx.transacaoRecorrenteId) : '',
      categoriaId: String(tx.categoriaId),
      descricao: tx.descricao,
      valor: String(tx.valor),
      tipoMovimentacao: tx.tipoMovimentacao,
      tipoPagamento: tx.tipoPagamento,
      data: tx.data,
    });
    this.modalErrorMessage.set('');
  }

  closeModal(): void {
    this.modalOpen.set(false);
    this.resetForm();
  }

  onRecorrenteChange(rawId: string): void {
    const recorrenteId = Number(rawId);
    if (!Number.isFinite(recorrenteId) || recorrenteId < 1) {
      return;
    }

    const recorrente = this.recorrentes().find((item) => item.id === recorrenteId);
    if (!recorrente) {
      return;
    }

    const periodo = this.selectedPeriodo();
    const dataPadrao = periodo
      ? this.clampDateToPeriodo(recorrente.dataInicio, periodo)
      : recorrente.dataInicio;

    this.form.patchValue({
      categoriaId: String(recorrente.categoriaId),
      descricao: recorrente.descricao,
      valor: String(recorrente.valorParcela),
      tipoMovimentacao: recorrente.tipoMovimentacao,
      tipoPagamento: recorrente.tipoPagamento,
      data: dataPadrao,
    });
  }

  isManualMode(): boolean {
    return !this.form.controls.transacaoRecorrenteId.value;
  }

  fieldError(control: AbstractControl | null, label: string): string {
    if (!control || !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return `${label} obrigatorio`;
    }

    if (control.hasError('maxlength')) {
      return `${label} deve ter no maximo 255 caracteres`;
    }

    if (control.hasError('min')) {
      return `${label} deve ser maior que zero`;
    }

    return '';
  }

  submitTransacao(): void {
    if (this.submitting()) {
      return;
    }

    const periodo = this.selectedPeriodo();
    if (!periodo) {
      this.modalErrorMessage.set('Selecione um periodo valido para lancar a transacao.');
      return;
    }

    if (this.form.controls.data.invalid) {
      this.form.controls.data.markAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const recorrenteId = raw.transacaoRecorrenteId ? Number(raw.transacaoRecorrenteId) : null;

    if (!recorrenteId) {
      if (!raw.categoriaId || !raw.descricao.trim() || !raw.valor || !raw.tipoMovimentacao || !raw.tipoPagamento) {
        this.modalErrorMessage.set('Preencha categoria, descricao, valor, tipo de movimentacao e tipo de pagamento.');
        return;
      }
    }

    this.modalErrorMessage.set('');
    this.submitting.set(true);

    const payload = {
      categoriaId: recorrenteId ? null : Number(raw.categoriaId),
      descricao: recorrenteId ? null : raw.descricao.trim(),
      valor: recorrenteId ? null : Number(raw.valor),
      tipoMovimentacao: recorrenteId ? null : (raw.tipoMovimentacao as TipoMovimentacao),
      tipoPagamento: recorrenteId ? null : (raw.tipoPagamento as TipoPagamento),
      periodoId: periodo.id,
      transacaoRecorrenteId: recorrenteId,
      data: raw.data,
    };

    const editingId = this.editingId();
    const request$ = editingId
      ? this.transacoesApi.update(editingId, payload)
      : this.transacoesApi.create(payload);

    request$.subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.show(editingId ? 'Transacao atualizada.' : 'Transacao criada.', 'success');
        this.closeModal();
        this.reloadSelectedPeriodo();
      },
      error: (err) => {
        this.modalErrorMessage.set(mapApiError(err));
        this.submitting.set(false);
      },
    });
  }

  deleteTransacao(tx: TransacaoResponse): void {
    if (this.deletingId()) {
      return;
    }

    const confirmDelete = window.confirm(`Excluir a transacao "${tx.descricao || 'Sem descricao'}"?`);
    if (!confirmDelete) {
      return;
    }

    this.deletingId.set(tx.id);

    this.transacoesApi.delete(tx.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.toast.show('Transacao excluida.', 'success');
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

    this.periodosApi.listAll().subscribe({
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

    this.transacoesApi.listByPeriodo(periodo.id, periodo.dataInicio, periodo.dataFim).subscribe({
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

    this.categoriasApi.listAll().subscribe({
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

    this.transacoesRecorrentesApi.listAll().subscribe({
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

  private clampDateToPeriodo(value: string, periodo: PeriodoFinanceiro): string {
    if (value < periodo.dataInicio) {
      return periodo.dataInicio;
    }
    if (value > periodo.dataFim) {
      return periodo.dataFim;
    }
    return value;
  }

  private calcularNumeroParcela(recorrente: TransacaoRecorrenteResponse, data: string): number | null {
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

  private proximaData(dataIso: string, frequencia: TransacaoRecorrenteResponse['frequencia']): string {
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
      return this.toIsoDate(next);
    } else {
      const day = date.getDate();
      const month = date.getMonth();
      const year = date.getFullYear() + 1;
      const maxDay = new Date(year, month + 1, 0).getDate();
      return this.toIsoDate(new Date(year, month, Math.min(day, maxDay)));
    }

    return this.toIsoDate(date);
  }

  private findPeriodoAtual(periodos: PeriodoFinanceiro[]): PeriodoFinanceiro | null {
    const nowIso = this.toIsoDate(new Date());

    return periodos.find((periodo) => {
      return nowIso >= periodo.dataInicio && nowIso <= periodo.dataFim;
    }) ?? null;
  }

  private toIsoDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private resetForm(): void {
    this.editingId.set(null);
    this.form.setValue({
      transacaoRecorrenteId: '',
      categoriaId: '',
      descricao: '',
      valor: '',
      tipoMovimentacao: '',
      tipoPagamento: '',
      data: '',
    });
    this.form.markAsPristine();
    this.form.markAsUntouched();
    this.modalErrorMessage.set('');
  }

}

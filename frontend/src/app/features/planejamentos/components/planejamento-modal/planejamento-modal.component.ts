import { Component, computed, DestroyRef, effect, inject, input, OnInit, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { startWith } from 'rxjs';

import {
  CategoriaResponse,
  CLASSIFICACAO_LABELS,
} from '../../../../core/models/categoria.models';
import { NaturezaFinanceira } from '../../../../core/models/natureza-financeira.models';
import { PeriodoFinanceiro } from '../../../../core/models/periodo-financeiro.models';
import { PlanejamentoResponse } from '../../../../core/models/planejamento.models';
import { TransacaoRecorrenteResponse } from '../../../../core/models/transacao-recorrente.models';
import {
  TIPOS_MOVIMENTACAO,
} from '../../../../core/models/transacao.models';
import { PlanejamentosApiService } from '../../../../core/services/planejamentos-api.service';
import { ToastService } from '../../../../core/services/toast.service';
import { CurrencyInputDirective } from '../../../../shared/directives/currency-input.directive';
import { mapApiError } from '../../../../shared/utils/error-message.util';
import { fieldError } from '../../../../shared/utils/form-error.util';
import { parseCurrencyInput, toCurrencyInputValue } from '../../../../shared/utils/format.util';
import { currencyAmountValidator } from '../../../../shared/validators/br-validators';
import { CategoriaModalComponent } from '../../../categorias/components/categoria-modal/categoria-modal.component';

const CRIAR_NOVA_CATEGORIA = 'CRIAR_NOVA';

@Component({
  selector: 'app-planejamento-modal',
  imports: [ReactiveFormsModule, CategoriaModalComponent, CurrencyInputDirective],
  templateUrl: './planejamento-modal.component.html',
})
export class PlanejamentoModalComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly planejamentosApi = inject(PlanejamentosApiService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly categorias = input.required<CategoriaResponse[]>();
  readonly recorrentes = input.required<TransacaoRecorrenteResponse[]>();
  readonly planejamentos = input.required<PlanejamentoResponse[]>();
  readonly selectedPeriodo = input.required<PeriodoFinanceiro>();
  readonly editingPlanejamento = input<PlanejamentoResponse | null>(null);

  readonly saved = output<void>();
  readonly closed = output<void>();
  readonly categoriasChanged = output<CategoriaResponse>();

  readonly tiposMovimentacao = TIPOS_MOVIMENTACAO;
  readonly classificacaoLabels = CLASSIFICACAO_LABELS;
  readonly fieldError = fieldError;
  readonly criarNovaCategoriaValue = CRIAR_NOVA_CATEGORIA;
  readonly submitting = signal(false);
  readonly errorMessage = signal('');
  readonly editingId = computed(() => this.editingPlanejamento()?.id ?? null);
  readonly tipoMovimentacaoSelecionado = signal<'' | NaturezaFinanceira>('');
  readonly categoriaModalOpen = signal(false);
  readonly categoriasLocais = signal<CategoriaResponse[]>([]);

  readonly recorrentesDisponiveis = computed(() => {
    const descricoesLancadas = new Set(
      this.planejamentos().map((item) => item.descricao.trim().toLowerCase())
    );
    return this.recorrentes().filter(
      (recorrente) => !descricoesLancadas.has(recorrente.descricao.trim().toLowerCase())
    );
  });

  readonly form = this.formBuilder.nonNullable.group({
    transacaoRecorrenteId: [''],
    categoriaId: ['', Validators.required],
    descricao: ['', [Validators.required, Validators.maxLength(255)]],
    valor: ['', [currencyAmountValidator(0.01)]],
    tipoMovimentacao: ['' as '' | NaturezaFinanceira, Validators.required],
  });

  constructor() {
    effect(() => {
      this.categoriasLocais.set(this.categorias());
    });
  }

  ngOnInit(): void {
    const item = this.editingPlanejamento();
    if (item) {
      this.form.setValue({
        transacaoRecorrenteId: '',
        categoriaId: String(item.categoriaId),
        descricao: item.descricao,
        valor: toCurrencyInputValue(item.valor),
        tipoMovimentacao: item.tipoMovimentacao,
      });
    }

    this.form.controls.tipoMovimentacao.valueChanges
      .pipe(
        startWith(this.form.controls.tipoMovimentacao.value),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((tipo) => {
        this.tipoMovimentacaoSelecionado.set(tipo);
        const categoria = this.categoriasLocais().find(
          (item) => item.id === Number(this.form.controls.categoriaId.value)
        );
        if (categoria && categoria.tipoCategoria !== tipo) {
          this.form.controls.categoriaId.setValue('');
        }
      });
  }

  categoriasDisponiveis(): CategoriaResponse[] {
    const tipo = this.tipoMovimentacaoSelecionado();
    return tipo ? this.categoriasLocais().filter((categoria) => categoria.tipoCategoria === tipo) : [];
  }

  onCategoriaChange(rawValue: string): void {
    if (rawValue !== CRIAR_NOVA_CATEGORIA) {
      return;
    }

    this.form.controls.categoriaId.setValue('');
    this.categoriaModalOpen.set(true);
  }

  closeCategoriaModal(): void {
    this.categoriaModalOpen.set(false);
  }

  onCategoriaSaved(categoria: CategoriaResponse): void {
    this.categoriasLocais.update((lista) =>
      lista.some((item) => item.id === categoria.id) ? lista : [...lista, categoria]
    );
    this.form.controls.categoriaId.setValue(String(categoria.id));
    this.categoriasChanged.emit(categoria);
    this.closeCategoriaModal();
  }

  tipoCategoriaInicial(): NaturezaFinanceira | null {
    const tipo = this.form.controls.tipoMovimentacao.value;
    return tipo || null;
  }

  onRecorrenteChange(rawId: string): void {
    const recorrente = this.recorrentesDisponiveis().find((item) => item.id === Number(rawId));
    if (!recorrente) {
      return;
    }

    this.form.patchValue({
      categoriaId: String(recorrente.categoriaId),
      descricao: recorrente.descricao,
      valor: toCurrencyInputValue(recorrente.valorParcela),
      tipoMovimentacao: recorrente.tipoMovimentacao,
    });
  }

  recorrenteLabel(recorrente: TransacaoRecorrenteResponse): string {
    return `${recorrente.descricao} - ${recorrente.categoriaNome}`;
  }

  categoriaLabel(categoria: CategoriaResponse): string {
    return categoria.classificacao
      ? `${categoria.nome} - ${this.classificacaoLabels[categoria.classificacao]}`
      : categoria.nome;
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const recorrenteId = raw.transacaoRecorrenteId ? Number(raw.transacaoRecorrenteId) : null;
    const payload = {
      categoriaId: Number(raw.categoriaId),
      periodoId: this.selectedPeriodo().id,
      descricao: raw.descricao.trim(),
      valor: parseCurrencyInput(raw.valor) ?? 0,
      tipoMovimentacao: raw.tipoMovimentacao as NaturezaFinanceira,
      transacaoRecorrenteId: this.editingId() ? null : recorrenteId,
    };
    const editingId = this.editingId();
    const request$ = editingId
      ? this.planejamentosApi.update(editingId, payload)
      : this.planejamentosApi.create(payload);

    this.submitting.set(true);
    this.errorMessage.set('');
    request$.subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.show(editingId ? 'Planejamento atualizado.' : 'Planejamento criado.', 'success');
        this.saved.emit();
      },
      error: (err) => {
        this.errorMessage.set(mapApiError(err));
        this.submitting.set(false);
      },
    });
  }

  close(): void {
    this.closed.emit();
  }
}

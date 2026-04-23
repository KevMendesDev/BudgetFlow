import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CategoriaResponse } from '../../../../core/models/categoria.models';
import {
  Frequencia,
  TipoMovimentacao,
  TipoPagamento,
  TransacaoRecorrenteResponse,
} from '../../../../core/models/transacao-recorrente.models';
import { CategoriasApiService } from '../../../../core/services/categorias-api.service';
import { ToastService } from '../../../../core/services/toast.service';
import { TransacoesRecorrentesApiService } from '../../../../core/services/transacoes-recorrentes-api.service';
import { mapApiError } from '../../../../shared/utils/error-message.util';

@Component({
  selector: 'app-transacoes-recorrentes-page',
  imports: [ReactiveFormsModule],
  templateUrl: './transacoes-recorrentes-page.component.html',
  styleUrl: './transacoes-recorrentes-page.component.scss',
})
export class TransacoesRecorrentesPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly recorrentesApi = inject(TransacoesRecorrentesApiService);
  private readonly categoriasApi = inject(CategoriasApiService);
  private readonly toast = inject(ToastService);

  readonly recorrencias = signal<TransacaoRecorrenteResponse[]>([]);
  readonly categorias = signal<CategoriaResponse[]>([]);
  readonly loading = signal(false);
  readonly loadingCategorias = signal(false);
  readonly submitting = signal(false);
  readonly deletingId = signal<number | null>(null);
  readonly modalOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly errorMessage = signal('');
  readonly modalErrorMessage = signal('');

  readonly frequencias: Array<{ value: Frequencia; label: string }> = [
    { value: 'DIARIO', label: 'Diario' },
    { value: 'SEMANAL', label: 'Semanal' },
    { value: 'MENSAL', label: 'Mensal' },
    { value: 'ANUAL', label: 'Anual' },
  ];

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

  readonly filtersForm = this.formBuilder.nonNullable.group({
    query: [''],
    frequencia: ['' as '' | Frequencia],
    tipoMovimentacao: ['' as '' | TipoMovimentacao],
  });

  readonly form = this.formBuilder.nonNullable.group({
    categoriaId: ['', [Validators.required]],
    descricao: ['', [Validators.required, Validators.maxLength(255)]],
    valorParcela: ['', [Validators.required, Validators.min(0.01)]],
    tipoMovimentacao: ['' as '' | TipoMovimentacao, [Validators.required]],
    tipoPagamento: ['' as '' | TipoPagamento, [Validators.required]],
    frequencia: ['' as '' | Frequencia, [Validators.required]],
    dataInicio: ['', [Validators.required]],
    dataFim: [''],
    totalParcelas: ['', [Validators.min(1)]],
  });

  constructor() {
    this.loadCategorias();
    this.loadRecorrencias();
  }

  applyFilters(): void {
    this.loadRecorrencias();
  }

  clearFilters(): void {
    this.filtersForm.setValue({ query: '', frequencia: '', tipoMovimentacao: '' });
    this.loadRecorrencias();
  }

  openCreateModal(): void {
    this.modalOpen.set(true);
    this.resetForm();
  }

  startEdit(item: TransacaoRecorrenteResponse): void {
    this.modalOpen.set(true);
    this.editingId.set(item.id);
    this.form.setValue({
      categoriaId: String(item.categoriaId),
      descricao: item.descricao,
      valorParcela: String(item.valorParcela),
      tipoMovimentacao: item.tipoMovimentacao,
      tipoPagamento: item.tipoPagamento,
      frequencia: item.frequencia,
      dataInicio: item.dataInicio,
      dataFim: item.dataFim ?? '',
      totalParcelas: item.totalParcelas ? String(item.totalParcelas) : '',
    });
    this.modalErrorMessage.set('');
  }

  closeModal(): void {
    this.modalOpen.set(false);
    this.resetForm();
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
      return `${label} invalido`;
    }

    return '';
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.modalErrorMessage.set('');
    this.submitting.set(true);

    const raw = this.form.getRawValue();
    const payload = {
      categoriaId: Number(raw.categoriaId),
      descricao: raw.descricao.trim(),
      valorParcela: Number(raw.valorParcela),
      tipoMovimentacao: raw.tipoMovimentacao as TipoMovimentacao,
      tipoPagamento: raw.tipoPagamento as TipoPagamento,
      frequencia: raw.frequencia as Frequencia,
      dataInicio: raw.dataInicio,
      dataFim: raw.dataFim || null,
      totalParcelas: raw.totalParcelas ? Number(raw.totalParcelas) : null,
    };

    const editingId = this.editingId();
    const request$ = editingId
      ? this.recorrentesApi.update(editingId, payload)
      : this.recorrentesApi.create(payload);

    request$.subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.show(editingId ? 'Recorrencia atualizada.' : 'Recorrencia criada.', 'success');
        this.closeModal();
        this.loadRecorrencias();
      },
      error: (err) => {
        this.modalErrorMessage.set(mapApiError(err));
        this.submitting.set(false);
      },
    });
  }

  deleteRecorrencia(item: TransacaoRecorrenteResponse): void {
    if (this.deletingId()) {
      return;
    }

    const confirmDelete = window.confirm(`Excluir a recorrencia "${item.descricao}"?`);
    if (!confirmDelete) {
      return;
    }

    this.deletingId.set(item.id);

    this.recorrentesApi.delete(item.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.toast.show('Recorrencia excluida.', 'success');
        this.loadRecorrencias();
      },
      error: (err) => {
        this.deletingId.set(null);
        this.toast.show(mapApiError(err), 'error');
      },
    });
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      minimumFractionDigits: 2,
    }).format(value);
  }

  formatDate(value: string | null): string {
    if (!value) {
      return '-';
    }

    return new Date(`${value}T00:00:00`).toLocaleDateString('pt-BR');
  }

  frequenciaLabel(value: Frequencia): string {
    return this.frequencias.find((item) => item.value === value)?.label ?? value;
  }

  tipoMovimentacaoLabel(value: TipoMovimentacao): string {
    return this.tiposMovimentacao.find((item) => item.value === value)?.label ?? value;
  }

  tipoPagamentoLabel(value: TipoPagamento): string {
    return this.tiposPagamento.find((item) => item.value === value)?.label ?? value;
  }

  private loadCategorias(): void {
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

  private loadRecorrencias(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    const filters = this.filtersForm.getRawValue();

    this.recorrentesApi
      .listAll({
        query: filters.query,
        frequencia: filters.frequencia,
        tipoMovimentacao: filters.tipoMovimentacao,
      })
      .subscribe({
        next: (page) => {
          this.recorrencias.set(page.content);
          this.loading.set(false);
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
          this.loading.set(false);
        },
      });
  }

  private resetForm(): void {
    this.editingId.set(null);
    this.form.setValue({
      categoriaId: '',
      descricao: '',
      valorParcela: '',
      tipoMovimentacao: '',
      tipoPagamento: '',
      frequencia: '',
      dataInicio: '',
      dataFim: '',
      totalParcelas: '',
    });
    this.form.markAsPristine();
    this.form.markAsUntouched();
    this.modalErrorMessage.set('');
  }
}

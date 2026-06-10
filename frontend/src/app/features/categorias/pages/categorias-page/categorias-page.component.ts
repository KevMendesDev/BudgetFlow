import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import {
  CLASSIFICACAO_LABELS,
  CLASSIFICACOES,
  CategoriaResponse,
  ClassificacaoCategoria,
  TIPO_CATEGORIA_LABELS,
  TIPOS_CATEGORIA,
} from '../../../../core/models/categoria.models';
import { NaturezaFinanceira } from '../../../../core/models/natureza-financeira.models';
import { CategoriasApiService } from '../../../../core/services/categorias-api.service';
import { ToastService } from '../../../../core/services/toast.service';
import { ConfirmDialogService } from '../../../../shared/services/confirm-dialog.service';
import { mapApiError } from '../../../../shared/utils/error-message.util';
import { fieldError } from '../../../../shared/utils/form-error.util';
import { isDesktopViewport } from '../../../../shared/utils/viewport.util';

@Component({
  selector: 'app-categorias-page',
  imports: [ReactiveFormsModule],
  templateUrl: './categorias-page.component.html',
  styleUrl: './categorias-page.component.scss',
})
export class CategoriasPageComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly categoriasApi = inject(CategoriasApiService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly destroyRef = inject(DestroyRef);

  readonly categorias = signal<CategoriaResponse[]>([]);
  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly deletingId = signal<number | null>(null);
  readonly modalOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly errorMessage = signal('');
  readonly modalErrorMessage = signal('');
  readonly filtrosAbertos = signal(isDesktopViewport());
  readonly classificacoes = CLASSIFICACOES;
  readonly tiposCategoria = TIPOS_CATEGORIA;
  readonly fieldError = fieldError;

  readonly filtersForm = this.formBuilder.nonNullable.group({
    q: [''],
    classificacao: ['' as '' | ClassificacaoCategoria],
    tipoCategoria: ['' as '' | NaturezaFinanceira],
  });

  readonly form = this.formBuilder.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(100)]],
    tipoCategoria: [NaturezaFinanceira.DESPESA as NaturezaFinanceira, [Validators.required]],
    classificacao: ['' as '' | ClassificacaoCategoria],
  });

  constructor() {
    this.configureClassificacaoValidator(this.form.controls.tipoCategoria.value);

    this.form.controls.tipoCategoria.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((tipoCategoria) => this.configureClassificacaoValidator(tipoCategoria));
  }

  ngOnInit(): void {
    this.loadCategorias();

    this.filtersForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => this.loadCategorias());
  }

  toggleFiltros(): void {
    this.filtrosAbertos.update((v) => !v);
  }

  applyFilters(): void {
    this.loadCategorias();
  }

  clearFilters(): void {
    this.filtersForm.setValue({ q: '', classificacao: '', tipoCategoria: '' }, { emitEvent: false });
    this.loadCategorias();
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
      nome: raw.nome.trim(),
      tipoCategoria: raw.tipoCategoria as NaturezaFinanceira,
      classificacao:
        raw.tipoCategoria === NaturezaFinanceira.DESPESA
          ? (raw.classificacao as ClassificacaoCategoria)
          : null,
    };

    const editingId = this.editingId();
    const request$ = editingId
      ? this.categoriasApi.update(editingId, payload)
      : this.categoriasApi.create(payload);

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.show(editingId ? 'Categoria atualizada.' : 'Categoria criada.', 'success');
        this.closeModal();
        this.loadCategorias();
      },
      error: (err) => {
        this.modalErrorMessage.set(mapApiError(err));
        this.submitting.set(false);
      },
    });
  }

  openCreateModal(): void {
    this.modalOpen.set(true);
    this.resetForm();
  }

  startEdit(categoria: CategoriaResponse): void {
    this.modalOpen.set(true);
    this.editingId.set(categoria.id);
    this.form.setValue({
      nome: categoria.nome,
      tipoCategoria: categoria.tipoCategoria,
      classificacao: categoria.classificacao ?? '',
    });
    this.modalErrorMessage.set('');
  }

  closeModal(): void {
    this.modalOpen.set(false);
    this.resetForm();
  }

  async deleteCategoria(categoria: CategoriaResponse): Promise<void> {
    if (this.deletingId()) {
      return;
    }

    const confirmed = await this.confirmDialog.confirm(`Excluir a categoria "${categoria.nome}"?`);
    if (!confirmed) {
      return;
    }

    this.deletingId.set(categoria.id);

    this.categoriasApi
      .delete(categoria.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.deletingId.set(null);
          this.toast.show('Categoria excluída.', 'success');

          if (this.editingId() === categoria.id) {
            this.resetForm();
          }

          this.loadCategorias();
        },
        error: () => {
          this.deletingId.set(null);
        },
      });
  }

  classificacaoLabel(value: ClassificacaoCategoria | null): string {
    if (!value) {
      return 'Sem classificação';
    }
    return CLASSIFICACAO_LABELS[value] ?? value;
  }

  tipoCategoriaLabel(value: NaturezaFinanceira): string {
    return TIPO_CATEGORIA_LABELS[value] ?? value;
  }

  isDespesaForm(): boolean {
    return this.form.controls.tipoCategoria.value === NaturezaFinanceira.DESPESA;
  }

  private loadCategorias(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    const filters = this.filtersForm.getRawValue();

    this.categoriasApi
      .listAll({
        q: filters.q,
        classificacao: filters.classificacao,
        tipoCategoria: filters.tipoCategoria,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.categorias.set(page.content);
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
    this.form.setValue({ nome: '', tipoCategoria: NaturezaFinanceira.DESPESA, classificacao: '' });
    this.form.markAsPristine();
    this.form.markAsUntouched();
    this.modalErrorMessage.set('');
  }

  private configureClassificacaoValidator(tipoCategoria: NaturezaFinanceira): void {
    const classificacaoControl = this.form.controls.classificacao;
    if (tipoCategoria === NaturezaFinanceira.DESPESA) {
      classificacaoControl.setValidators([Validators.required]);
    } else {
      classificacaoControl.setValue('', { emitEvent: false });
      classificacaoControl.clearValidators();
    }
    classificacaoControl.updateValueAndValidity({ emitEvent: false });
  }
}

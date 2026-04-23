import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CategoriaResponse, ClassificacaoCategoria } from '../../../../core/models/categoria.models';
import { CategoriasApiService } from '../../../../core/services/categorias-api.service';
import { ToastService } from '../../../../core/services/toast.service';
import { mapApiError } from '../../../../shared/utils/error-message.util';

@Component({
  selector: 'app-categorias-page',
  imports: [ReactiveFormsModule],
  templateUrl: './categorias-page.component.html',
  styleUrl: './categorias-page.component.scss',
})
export class CategoriasPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly categoriasApi = inject(CategoriasApiService);
  private readonly toast = inject(ToastService);

  readonly categorias = signal<CategoriaResponse[]>([]);
  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly deletingId = signal<number | null>(null);
  readonly modalOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly errorMessage = signal('');
  readonly modalErrorMessage = signal('');
  readonly classificacoes: Array<{ value: ClassificacaoCategoria; label: string }> = [
    { value: 'ESSENCIAL', label: 'Essencial' },
    { value: 'NAO_ESSENCIAL', label: 'Nao essencial' },
    { value: 'INVESTIMENTO', label: 'Investimento' },
  ];

  readonly filtersForm = this.formBuilder.nonNullable.group({
    q: [''],
    classificacao: ['' as '' | ClassificacaoCategoria],
  });

  readonly form = this.formBuilder.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(100)]],
    classificacao: ['' as '' | ClassificacaoCategoria, [Validators.required]],
  });

  constructor() {
    this.loadCategorias();
  }

  applyFilters(): void {
    this.loadCategorias();
  }

  clearFilters(): void {
    this.filtersForm.setValue({ q: '', classificacao: '' });
    this.loadCategorias();
  }

  fieldError(control: AbstractControl | null, label: string): string {
    if (!control || !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return `${label} obrigatorio`;
    }

    if (control.hasError('maxlength')) {
      return `${label} deve ter no maximo 100 caracteres`;
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
      nome: raw.nome.trim(),
      classificacao: raw.classificacao as ClassificacaoCategoria,
    };

    const editingId = this.editingId();
    const request$ = editingId
      ? this.categoriasApi.update(editingId, payload)
      : this.categoriasApi.create(payload);

    request$.subscribe({
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
      classificacao: categoria.classificacao,
    });
    this.modalErrorMessage.set('');
  }

  closeModal(): void {
    this.modalOpen.set(false);
    this.resetForm();
  }

  deleteCategoria(categoria: CategoriaResponse): void {
    if (this.deletingId()) {
      return;
    }

    const confirmDelete = window.confirm(`Excluir a categoria "${categoria.nome}"?`);
    if (!confirmDelete) {
      return;
    }

    this.deletingId.set(categoria.id);

    this.categoriasApi.delete(categoria.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.toast.show('Categoria excluida.', 'success');

        if (this.editingId() === categoria.id) {
          this.resetForm();
        }

        this.loadCategorias();
      },
      error: (err) => {
        this.deletingId.set(null);
        this.toast.show(mapApiError(err), 'error');
      },
    });
  }

  classificacaoLabel(value: ClassificacaoCategoria): string {
    const match = this.classificacoes.find((item) => item.value === value);
    return match?.label ?? value;
  }

  private loadCategorias(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    const filters = this.filtersForm.getRawValue();

    this.categoriasApi
      .listAll({
        q: filters.q,
        classificacao: filters.classificacao,
      })
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
    this.form.setValue({ nome: '', classificacao: '' });
    this.form.markAsPristine();
    this.form.markAsUntouched();
    this.modalErrorMessage.set('');
  }
}

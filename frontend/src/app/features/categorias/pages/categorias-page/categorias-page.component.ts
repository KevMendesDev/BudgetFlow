import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
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
import { PageSize } from '../../../../core/models/pagination.models';
import { CategoriasApiService } from '../../../../core/services/categorias-api.service';
import { ToastService } from '../../../../core/services/toast.service';
import { ConfirmDialogService } from '../../../../shared/services/confirm-dialog.service';
import { mapApiError } from '../../../../shared/utils/error-message.util';
import { isDesktopViewport } from '../../../../shared/utils/viewport.util';
import { CategoriaModalComponent } from '../../components/categoria-modal/categoria-modal.component';

@Component({
  selector: 'app-categorias-page',
  imports: [ReactiveFormsModule, CategoriaModalComponent],
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
  readonly deletingId = signal<number | null>(null);
  readonly modalOpen = signal(false);
  readonly editingCategoria = signal<CategoriaResponse | null>(null);
  readonly errorMessage = signal('');
  readonly filtrosAbertos = signal(isDesktopViewport());
  readonly paginaAtual = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly classificacoes = CLASSIFICACOES;
  readonly tiposCategoria = TIPOS_CATEGORIA;

  readonly filtersForm = this.formBuilder.nonNullable.group({
    q: [''],
    classificacao: ['' as '' | ClassificacaoCategoria],
    tipoCategoria: ['' as '' | NaturezaFinanceira],
  });

  ngOnInit(): void {
    this.loadCategorias(0);

    this.filtersForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => this.loadCategorias(0));
  }

  toggleFiltros(): void {
    this.filtrosAbertos.update((v) => !v);
  }

  applyFilters(): void {
    this.loadCategorias(0);
  }

  clearFilters(): void {
    this.filtersForm.setValue({ q: '', classificacao: '', tipoCategoria: '' }, { emitEvent: false });
    this.loadCategorias(0);
  }

  openCreateModal(): void {
    this.editingCategoria.set(null);
    this.modalOpen.set(true);
  }

  startEdit(categoria: CategoriaResponse): void {
    this.editingCategoria.set(categoria);
    this.modalOpen.set(true);
  }

  closeModal(): void {
    this.modalOpen.set(false);
    this.editingCategoria.set(null);
  }

  onCategoriaSaved(_categoria?: CategoriaResponse): void {
    this.closeModal();
    this.loadCategorias(this.paginaAtual());
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
          this.loadCategorias(this.paginaAtual());
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
  goToPreviousPage(): void {
    const atual = this.paginaAtual();
    if (atual > 0) {
      this.loadCategorias(atual - 1);
    }
  }

  goToNextPage(): void {
    const atual = this.paginaAtual();
    if (atual + 1 < this.totalPages()) {
      this.loadCategorias(atual + 1);
    }
  }

  private loadCategorias(page = this.paginaAtual()): void {
    this.loading.set(true);
    this.errorMessage.set('');

    const filters = this.filtersForm.getRawValue();

    this.categoriasApi
      .listAll({
        page,
        size: PageSize.DEFAULT,
        q: filters.q,
        classificacao: filters.classificacao,
        tipoCategoria: filters.tipoCategoria,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          const totalPages = page.page.totalPages ?? page.totalPages ?? 0;
          const totalElements = page.page.totalElements ?? page.totalElements ?? 0;

          if (page.content.length === 0 && totalPages > 0 && this.paginaAtual() >= totalPages) {
            this.loadCategorias(totalPages - 1);
            return;
          }

          this.categorias.set(page.content);
          this.paginaAtual.set(page.page.number ?? page.number ?? 0);
          this.totalPages.set(totalPages);
          this.totalElements.set(totalElements);
          this.loading.set(false);
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
          this.totalPages.set(0);
          this.totalElements.set(0);
          this.loading.set(false);
        },
      });
  }
}

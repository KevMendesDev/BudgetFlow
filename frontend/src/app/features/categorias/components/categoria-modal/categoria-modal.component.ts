import { Component, DestroyRef, inject, input, OnInit, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import {
  CategoriaResponse,
  ClassificacaoCategoria,
  CLASSIFICACOES,
  TIPOS_CATEGORIA,
} from '../../../../core/models/categoria.models';
import { NaturezaFinanceira } from '../../../../core/models/natureza-financeira.models';
import { CategoriasApiService } from '../../../../core/services/categorias-api.service';
import { ToastService } from '../../../../core/services/toast.service';
import { mapApiError } from '../../../../shared/utils/error-message.util';
import { fieldError } from '../../../../shared/utils/form-error.util';

@Component({
  selector: 'app-categoria-modal',
  imports: [ReactiveFormsModule],
  templateUrl: './categoria-modal.component.html',
})
export class CategoriaModalComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly categoriasApi = inject(CategoriasApiService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly editingCategoria = input<CategoriaResponse | null>(null);

  readonly saved = output<void>();
  readonly closed = output<void>();

  readonly classificacoes = CLASSIFICACOES;
  readonly tiposCategoria = TIPOS_CATEGORIA;
  readonly fieldError = fieldError;

  readonly submitting = signal(false);
  readonly errorMessage = signal('');

  readonly form = this.formBuilder.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(100)]],
    tipoCategoria: [NaturezaFinanceira.DESPESA as NaturezaFinanceira, [Validators.required]],
    classificacao: ['' as '' | ClassificacaoCategoria],
  });

  ngOnInit(): void {
    this.configureClassificacaoValidator(this.form.controls.tipoCategoria.value);

    this.form.controls.tipoCategoria.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((tipoCategoria) => this.configureClassificacaoValidator(tipoCategoria));

    const categoria = this.editingCategoria();
    if (categoria) {
      this.form.setValue({
        nome: categoria.nome,
        tipoCategoria: categoria.tipoCategoria,
        classificacao: categoria.classificacao ?? '',
      });
      return;
    }

    this.resetForm();
  }

  isDespesaForm(): boolean {
    return this.form.controls.tipoCategoria.value === NaturezaFinanceira.DESPESA;
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set('');
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

    const editingId = this.editingCategoria()?.id;
    const request$ = editingId
      ? this.categoriasApi.update(editingId, payload)
      : this.categoriasApi.create(payload);

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.show(editingId ? 'Categoria atualizada.' : 'Categoria criada.', 'success');
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

  private resetForm(): void {
    this.form.setValue({ nome: '', tipoCategoria: NaturezaFinanceira.DESPESA, classificacao: '' });
    this.form.markAsPristine();
    this.form.markAsUntouched();
    this.errorMessage.set('');
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

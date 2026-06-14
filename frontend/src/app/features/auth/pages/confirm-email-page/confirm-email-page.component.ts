import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthApiService } from '../../../../core/services/auth-api.service';
import { mapApiError } from '../../../../shared/utils/error-message.util';

@Component({
  selector: 'app-confirm-email-page',
  imports: [RouterLink],
  templateUrl: './confirm-email-page.component.html',
})
export class ConfirmEmailPageComponent implements OnInit {
  private readonly authApi = inject(AuthApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(true);
  readonly errorMessage = signal('');

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.loading.set(false);
      this.errorMessage.set('Link de confirmação inválido.');
      return;
    }

    this.authApi
      .confirmEmail(token)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.router.navigate(['/login'], { queryParams: { message: response.message } });
        },
        error: (error) => {
          this.loading.set(false);
          this.errorMessage.set(mapApiError(error));
        },
      });
  }
}

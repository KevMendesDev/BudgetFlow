import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthApiService } from '../../core/services/auth-api.service';
import { SessionService } from '../../core/services/session.service';

@Component({
  selector: 'app-private-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './private-layout.component.html',
  styleUrl: './private-layout.component.scss',
})
export class PrivateLayoutComponent {
  private readonly authApi = inject(AuthApiService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);

  readonly user = computed(() => this.session.user());
  readonly collapsed = signal(false);

  toggleSidebar(): void {
    this.collapsed.update((state) => !state);
  }

  logout(): void {
    this.authApi.logout().subscribe({
      next: () => {
        this.session.clearSession();
        this.router.navigate(['/login']);
      },
      error: () => {
        this.session.clearSession();
        this.router.navigate(['/login']);
      },
    });
  }
}

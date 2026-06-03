import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AuthApiService } from '../../core/services/auth-api.service';
import { SessionService } from '../../core/services/session.service';
import { isMobileViewport } from '../../shared/utils/viewport.util';

@Component({
  selector: 'app-private-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './private-layout.component.html',
  styleUrl: './private-layout.component.scss',
})
export class PrivateLayoutComponent implements OnInit {
  private readonly authApi = inject(AuthApiService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly user = computed(() => this.session.user());
  readonly collapsed = signal(isMobileViewport());

  ngOnInit(): void {
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        if (isMobileViewport()) {
          this.collapsed.set(true);
        }
      });
  }

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

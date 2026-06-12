import { Component, computed, inject } from '@angular/core';

import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast',
  templateUrl: './app-toast.component.html',
  styleUrl: './app-toast.component.scss',
})
export class AppToastComponent {
  private readonly toastService = inject(ToastService);

  readonly message = computed(() => this.toastService.message());

  close(): void {
    this.toastService.clear();
  }
}

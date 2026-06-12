import { Injectable, signal } from '@angular/core';

export type ToastType = 'error' | 'success' | 'info';

export interface ToastMessage {
  text: string;
  type: ToastType;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly message = signal<ToastMessage | null>(null);

  private hideTimeout: ReturnType<typeof setTimeout> | null = null;

  show(text: string, type: ToastType = 'error', durationMs = 3500): void {
    this.message.set({ text, type });

    if (this.hideTimeout) {
      clearTimeout(this.hideTimeout);
    }

    this.hideTimeout = setTimeout(() => {
      this.message.set(null);
      this.hideTimeout = null;
    }, durationMs);
  }

  clear(): void {
    if (this.hideTimeout) {
      clearTimeout(this.hideTimeout);
      this.hideTimeout = null;
    }

    this.message.set(null);
  }
}

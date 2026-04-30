import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  private resolver: ((confirmed: boolean) => void) | null = null;

  readonly message = signal('');
  readonly isOpen = signal(false);

  confirm(message: string): Promise<boolean> {
    this.message.set(message);
    this.isOpen.set(true);

    return new Promise<boolean>((resolve) => {
      this.resolver = resolve;
    });
  }

  resolve(confirmed: boolean): void {
    this.isOpen.set(false);
    this.resolver?.(confirmed);
    this.resolver = null;
  }
}

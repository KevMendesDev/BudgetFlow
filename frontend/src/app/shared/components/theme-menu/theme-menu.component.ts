import { Component, inject } from '@angular/core';

import { ThemeMode, ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-theme-menu',
  templateUrl: './theme-menu.component.html',
  styleUrl: './theme-menu.component.scss',
})
export class ThemeMenuComponent {
  private readonly theme = inject(ThemeService);

  readonly themeMode = this.theme.mode;

  setThemeMode(mode: ThemeMode, menu: HTMLDetailsElement): void {
    this.theme.setMode(mode);
    menu.open = false;
  }
}

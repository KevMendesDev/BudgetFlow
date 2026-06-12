import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { ThemeMenuComponent } from '../../shared/components/theme-menu/theme-menu.component';

@Component({
  selector: 'app-auth-layout',
  imports: [RouterOutlet, RouterLink, ThemeMenuComponent],
  templateUrl: './auth-layout.component.html',
  styleUrl: './auth-layout.component.scss',
})
export class AuthLayoutComponent {}

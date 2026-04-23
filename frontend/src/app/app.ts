import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AppToastComponent } from './shared/components/app-toast/app-toast.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, AppToastComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
}

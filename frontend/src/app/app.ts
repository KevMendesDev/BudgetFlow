import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { AppToastComponent } from './shared/components/app-toast/app-toast.component';
import { ConfirmDialogComponent } from './shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, AppToastComponent, ConfirmDialogComponent],
  templateUrl: './app.html',
})
export class App {}

import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { SessionResumeService } from './core/services/session-resume.service';
import { AppToastComponent } from './shared/components/app-toast/app-toast.component';
import { ConfirmDialogComponent } from './shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, AppToastComponent, ConfirmDialogComponent],
  templateUrl: './app.html',
})
export class App {
  constructor() {
    inject(SessionResumeService).start();
  }
}

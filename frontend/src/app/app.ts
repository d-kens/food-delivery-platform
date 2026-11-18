import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Toasts } from './shared/toast/toasts';

@Component({
  imports: [RouterOutlet, Toasts],
  selector: 'app-root',
  templateUrl: './app.html',
})
export class App {}

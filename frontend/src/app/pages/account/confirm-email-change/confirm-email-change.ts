import { Location } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroCheckCircle, heroEnvelope, heroLink } from '@ng-icons/heroicons/outline';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { genericErrorMessage, problemOf, statusOf } from '../../../core/http/problem-detail';
import { UsersService } from '../../../core/users/users.service';
import { FormAlert } from '../../../shared/forms/form-alert';
import { Spinner } from '../../../shared/spinner/spinner';

@Component({
  selector: 'app-confirm-email-change',
  imports: [RouterLink, NgIcon, FormAlert, Spinner],
  viewProviders: [provideIcons({ heroCheckCircle, heroEnvelope, heroLink })],
  templateUrl: './confirm-email-change.html',
})
export class ConfirmEmailChange {
  private readonly users = inject(UsersService);
  private readonly token = inject(ActivatedRoute).snapshot.queryParamMap.get('token');

  protected readonly auth = inject(AuthService);
  protected readonly linkInvalid = signal(!this.token);
  protected readonly changed = signal(false);
  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);

  constructor() {
    if (this.token) {
      inject(Location).replaceState('/account/email/confirm');
    }
  }

  protected confirm(): void {
    if (this.submitting() || !this.token) {
      return;
    }
    this.formError.set(null);
    this.submitting.set(true);
    this.users
      .confirmEmailChange(this.token)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => this.changed.set(true),
        error: (error) => {
          const status = statusOf(error);
          if (status === 400) {
            this.linkInvalid.set(true);
            return;
          }
          if (status === 409) {
            this.formError.set(
              `${problemOf(error).detail ?? 'An account with this email already exists'}. Request the change again with a different email.`,
            );
            return;
          }
          this.formError.set(genericErrorMessage(error));
        },
      });
  }
}

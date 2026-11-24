import { Location } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroExclamationTriangle, heroLink } from '@ng-icons/heroicons/outline';
import { finalize } from 'rxjs';
import { genericErrorMessage, statusOf } from '../../../core/http/problem-detail';
import { UsersService } from '../../../core/users/users.service';
import { FormAlert } from '../../../shared/forms/form-alert';
import { Spinner } from '../../../shared/spinner/spinner';

@Component({
  selector: 'app-confirm-account-deletion',
  imports: [RouterLink, NgIcon, FormAlert, Spinner],
  viewProviders: [provideIcons({ heroExclamationTriangle, heroLink })],
  templateUrl: './confirm-account-deletion.html',
})
export class ConfirmAccountDeletion {
  private readonly users = inject(UsersService);
  private readonly router = inject(Router);
  private readonly token = inject(ActivatedRoute).snapshot.queryParamMap.get('token');

  protected readonly linkInvalid = signal(!this.token);
  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);

  constructor() {
    if (this.token) {
      inject(Location).replaceState('/account/delete/confirm');
    }
  }

  protected confirm(): void {
    if (this.submitting() || !this.token) {
      return;
    }
    this.formError.set(null);
    this.submitting.set(true);
    this.users
      .confirmAccountDeletion(this.token)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => this.router.navigateByUrl('/login', { state: { notice: 'account-deleted' } }),
        error: (error) => {
          if (statusOf(error) === 400) {
            this.linkInvalid.set(true);
            return;
          }
          this.formError.set(genericErrorMessage(error));
        },
      });
  }
}

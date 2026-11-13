import { Component, DestroyRef, ElementRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { isNetworkError, problemOf, statusOf } from '../../../core/http/problem-detail';
import { UsersService } from '../../../core/users/users.service';
import { FieldMessage } from '../../../shared/forms/field-message';
import { focusFirstInvalid, notBlank, shouldShowError } from '../../../shared/forms/form-utils';
import { Spinner } from '../../../shared/spinner/spinner';

@Component({
  selector: 'app-delete-account',
  imports: [ReactiveFormsModule, FieldMessage, Spinner],
  templateUrl: './delete-account.html',
})
export class DeleteAccount {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly users = inject(UsersService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  private readonly passwordError = signal<string | null>(null);
  private readonly submitted = signal(false);

  protected readonly form = this.fb.group({
    currentPassword: ['', notBlank],
    confirm: [false, Validators.requiredTrue],
  });

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed(inject(DestroyRef))).subscribe(() => {
      this.formError.set(null);
      this.passwordError.set(null);
    });
  }

  protected passwordMessage(): string | null {
    if (this.passwordError()) {
      return this.passwordError();
    }
    return shouldShowError(this.form.controls.currentPassword, this.submitted())
      ? 'Enter your current password'
      : null;
  }

  protected confirmMessage(): string | null {
    return shouldShowError(this.form.controls.confirm, this.submitted())
      ? 'Tick the box to confirm you understand'
      : null;
  }

  protected onSubmit(): void {
    if (this.submitting()) {
      return;
    }
    this.submitted.set(true);
    if (this.form.invalid) {
      focusFirstInvalid(this.form, this.host);
      return;
    }

    this.submitting.set(true);
    this.users
      .deleteAccount(this.form.controls.currentPassword.value)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => this.router.navigateByUrl('/login', { state: { notice: 'account-deleted' } }),
        error: (error) => {
          if (statusOf(error) === 400 && !problemOf(error).errors) {
            this.passwordError.set(problemOf(error).detail ?? 'Current password is incorrect');
            this.host.querySelector<HTMLElement>('#currentPassword')?.focus();
            return;
          }
          this.formError.set(
            isNetworkError(error)
              ? "Can't reach Keroma right now. Check your connection and try again."
              : 'Something went wrong on our side. Please try again.',
          );
        },
      });
  }
}

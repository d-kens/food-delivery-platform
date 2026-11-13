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

type Field = 'currentPassword' | 'newPassword' | 'confirmPassword';

@Component({
  selector: 'app-password',
  imports: [ReactiveFormsModule, FieldMessage, Spinner],
  templateUrl: './password.html',
})
export class Password {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly users = inject(UsersService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  private readonly currentPasswordError = signal<string | null>(null);
  private readonly submitted = signal(false);

  protected readonly form = this.fb.group({
    currentPassword: ['', notBlank],
    newPassword: ['', [notBlank, Validators.minLength(8), Validators.maxLength(100)]],
    confirmPassword: ['', notBlank],
  });

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed(inject(DestroyRef))).subscribe(() => {
      this.formError.set(null);
      this.currentPasswordError.set(null);
    });
  }

  protected error(field: Field): string | null {
    if (field === 'currentPassword' && this.currentPasswordError()) {
      return this.currentPasswordError();
    }
    const control = this.form.controls[field];
    const mismatch =
      field === 'confirmPassword' &&
      control.value &&
      control.value !== this.form.controls.newPassword.value;
    if (mismatch && (control.touched || this.submitted())) {
      return "Passwords don't match";
    }
    if (!shouldShowError(control, this.submitted())) {
      return null;
    }
    if (control.hasError('required')) {
      return {
        currentPassword: 'Enter your current password',
        newPassword: 'Enter a new password',
        confirmPassword: 'Enter the new password again',
      }[field];
    }
    return 'Use between 8 and 100 characters';
  }

  protected onSubmit(): void {
    if (this.submitting()) {
      return;
    }
    this.submitted.set(true);
    const { currentPassword, newPassword, confirmPassword } = this.form.getRawValue();
    if (this.form.invalid || newPassword !== confirmPassword) {
      if (this.form.valid) {
        this.host.querySelector<HTMLElement>('#confirmPassword')?.focus();
      } else {
        focusFirstInvalid(this.form, this.host);
      }
      return;
    }

    this.submitting.set(true);
    this.users
      .changePassword(currentPassword, newPassword)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => this.router.navigateByUrl('/login', { state: { notice: 'password-changed' } }),
        error: (error) => {
          if (statusOf(error) === 400 && !problemOf(error).errors) {
            this.currentPasswordError.set(
              problemOf(error).detail ?? 'Current password is incorrect',
            );
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

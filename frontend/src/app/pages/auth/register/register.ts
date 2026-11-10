import { Component, DestroyRef, ElementRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroEye, heroEyeSlash } from '@ng-icons/heroicons/outline';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { isNetworkError, problemOf, statusOf } from '../../../core/http/problem-detail';
import { FieldMessage } from '../../../shared/forms/field-message';
import { focusFirstInvalid, isCapsLockOn, notBlank, shouldShowError } from '../../../shared/forms/form-utils';
import { GoogleButton } from '../../../shared/google-button/google-button';
import { Spinner } from '../../../shared/spinner/spinner';

type Field = 'firstName' | 'lastName' | 'email' | 'password';

const FIELDS: Field[] = ['firstName', 'lastName', 'email', 'password'];

const REQUIRED: Record<Field, string> = {
  firstName: 'Enter your first name',
  lastName: 'Enter your last name',
  email: 'Enter your email address',
  password: 'Create a password',
};

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, NgIcon, GoogleButton, FieldMessage, Spinner],
  viewProviders: [provideIcons({ heroEye, heroEyeSlash })],
  templateUrl: './register.html',
})
export class Register {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly showPassword = signal(false);
  protected readonly capsLock = signal(false);
  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  private readonly submitted = signal(false);

  protected readonly form = this.fb.group({
    firstName: ['', [notBlank, Validators.maxLength(100)]],
    lastName: ['', [notBlank, Validators.maxLength(100)]],
    email: ['', [notBlank, Validators.email, Validators.maxLength(255)]],
    password: ['', [notBlank, Validators.minLength(8), Validators.maxLength(100)]],
  });

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed(inject(DestroyRef))).subscribe(() => this.formError.set(null));
  }

  protected error(field: Field): string | null {
    const control = this.form.controls[field];
    if (!shouldShowError(control, this.submitted())) {
      return null;
    }
    if (control.hasError('required')) {
      return REQUIRED[field];
    }
    if (control.hasError('taken')) {
      return 'This email already has an account. Try logging in.';
    }
    if (control.hasError('server')) {
      return control.getError('server');
    }
    if (control.hasError('email')) {
      return 'Enter a valid email address';
    }
    if (control.hasError('minlength')) {
      return 'Use at least 8 characters';
    }
    return `Use ${control.getError('maxlength').requiredLength} characters or fewer`;
  }

  protected onPasswordKey(event: KeyboardEvent): void {
    this.capsLock.set(isCapsLockOn(event));
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

    const { firstName, lastName, email, password } = this.form.getRawValue();
    this.submitting.set(true);
    this.auth
      .register({ firstName: firstName.trim(), lastName: lastName.trim(), email, password })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () =>
          this.router.navigate(['/login'], {
            queryParamsHandling: 'preserve',
            state: { registeredEmail: email },
          }),
        error: (error) => this.handleError(error),
      });
  }

  private handleError(error: unknown): void {
    if (isNetworkError(error)) {
      this.formError.set("Can't reach Keroma right now. Check your connection and try again.");
      return;
    }

    const status = statusOf(error);
    if (status === 409) {
      this.form.controls.email.setErrors({ taken: true });
      focusFirstInvalid(this.form, this.host);
      return;
    }

    const fieldErrors = problemOf(error).errors;
    if (status === 400 && fieldErrors) {
      for (const field of FIELDS) {
        const message = fieldErrors[field]?.[0];
        if (message) {
          this.form.controls[field].setErrors({ server: message });
        }
      }
      focusFirstInvalid(this.form, this.host);
      return;
    }

    this.formError.set('Something went wrong on our side. Please try again.');
  }
}

import { Component, DestroyRef, ElementRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroEye, heroEyeSlash } from '@ng-icons/heroicons/outline';
import { finalize } from 'rxjs';
import { AuthService, safeRedirect } from '../../../core/auth/auth.service';
import { isNetworkError, statusOf } from '../../../core/http/problem-detail';
import { FieldMessage } from '../../../shared/forms/field-message';
import { focusFirstInvalid, isCapsLockOn, notBlank, shouldShowError } from '../../../shared/forms/form-utils';
import { GoogleButton } from '../../../shared/google-button/google-button';
import { Spinner } from '../../../shared/spinner/spinner';

type Field = 'email' | 'password';

const REQUIRED: Record<Field, string> = {
  email: 'Enter your email address',
  password: 'Enter your password',
};

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, NgIcon, GoogleButton, FieldMessage, Spinner],
  viewProviders: [provideIcons({ heroEye, heroEyeSlash })],
  templateUrl: './login.html',
})
export class Login {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly showPassword = signal(false);
  protected readonly capsLock = signal(false);
  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  private readonly submitted = signal(false);
  protected readonly justRegistered = signal(false);

  protected readonly form = this.fb.group({
    email: ['', [notBlank, Validators.email]],
    password: ['', notBlank],
  });

  constructor() {
    const registeredEmail = this.router.currentNavigation()?.extras.state?.['registeredEmail'];
    if (typeof registeredEmail === 'string') {
      this.justRegistered.set(true);
      this.form.controls.email.setValue(registeredEmail);
    }

    this.form.valueChanges.pipe(takeUntilDestroyed(inject(DestroyRef))).subscribe(() => this.formError.set(null));
  }

  protected error(field: Field): string | null {
    const control = this.form.controls[field];
    if (!shouldShowError(control, this.submitted())) {
      return null;
    }
    return control.hasError('required') ? REQUIRED[field] : 'Enter a valid email address';
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

    this.submitting.set(true);
    this.auth
      .login(this.form.getRawValue())
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => this.router.navigateByUrl(safeRedirect(this.route.snapshot.queryParamMap.get('next'))),
        error: (error) => this.formError.set(this.messageFor(error)),
      });
  }

  private messageFor(error: unknown): string {
    if (isNetworkError(error)) {
      return "Can't reach Keroma right now. Check your connection and try again.";
    }
    switch (statusOf(error)) {
      case 401:
        return 'Incorrect email or password.';
      case 403:
        return 'This account has been suspended. Contact support for help.';
      default:
        return 'Something went wrong on our side. Please try again.';
    }
  }
}

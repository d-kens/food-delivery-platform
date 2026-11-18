import { Component, DestroyRef, ElementRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroEnvelope } from '@ng-icons/heroicons/outline';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { genericErrorMessage } from '../../../core/http/problem-detail';
import { FieldMessage } from '../../../shared/forms/field-message';
import { FormAlert } from '../../../shared/forms/form-alert';
import { focusFirstInvalid, notBlank, shouldShowError } from '../../../shared/forms/form-utils';
import { Spinner } from '../../../shared/spinner/spinner';
import { ToastService } from '../../../shared/toast/toast.service';

@Component({
  selector: 'app-forgot-password',
  imports: [ReactiveFormsModule, RouterLink, NgIcon, FieldMessage, FormAlert, Spinner],
  viewProviders: [provideIcons({ heroEnvelope })],
  templateUrl: './forgot-password.html',
})
export class ForgotPassword {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly sentTo = signal<string | null>(null);
  private readonly submitted = signal(false);

  protected readonly form = this.fb.group({
    email: ['', [notBlank, Validators.email]],
  });

  constructor() {
    const email = this.router.currentNavigation()?.extras.state?.['email'];
    if (typeof email === 'string') {
      this.form.controls.email.setValue(email);
    }

    this.form.valueChanges
      .pipe(takeUntilDestroyed(inject(DestroyRef)))
      .subscribe(() => this.formError.set(null));
  }

  protected error(): string | null {
    const control = this.form.controls.email;
    if (!shouldShowError(control, this.submitted())) {
      return null;
    }
    return control.hasError('required')
      ? 'Enter your email address'
      : 'Enter a valid email address';
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

    const email = this.form.controls.email.value.trim();
    this.send(email, () => this.sentTo.set(email));
  }

  protected resend(): void {
    const email = this.sentTo();
    if (email && !this.submitting()) {
      this.send(email, () => this.toast.success(`We sent another link to ${email}.`));
    }
  }

  protected useDifferentEmail(): void {
    this.sentTo.set(null);
    this.submitted.set(false);
  }

  private send(email: string, onSent: () => void): void {
    this.submitting.set(true);
    this.auth
      .forgotPassword(email)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: onSent,
        error: (error) => {
          const message = genericErrorMessage(error);
          if (this.sentTo()) {
            this.toast.error(message);
          } else {
            this.formError.set(message);
          }
        },
      });
  }
}

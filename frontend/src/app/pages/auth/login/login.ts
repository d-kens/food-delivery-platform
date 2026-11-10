import { Component, ElementRef, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FieldMessage } from '../../../shared/forms/field-message';
import {
  focusFirstInvalid,
  isCapsLockOn,
  notBlank,
  shouldShowError,
} from '../../../shared/forms/form-utils';
import { GoogleButton } from '../../../shared/google-button/google-button';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroEye, heroEyeSlash } from '@ng-icons/heroicons/outline';

type Field = 'email' | 'password';

const REQUIRED: Record<Field, string> = {
  email: 'Enter your email address',
  password: 'Enter your password',
};

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, NgIcon, GoogleButton, FieldMessage],
  viewProviders: [provideIcons({ heroEye, heroEyeSlash })],
  templateUrl: './login.html',
})
export class Login {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;

  protected readonly showPassword = signal(false);
  protected readonly capsLock = signal(false);
  private readonly submitted = signal(false);

  // Mirrors AuthRequestDto validation in user-service.
  protected readonly form = this.fb.group({
    email: ['', [notBlank, Validators.email]],
    password: ['', notBlank],
  });

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
    this.submitted.set(true);
    if (this.form.invalid) {
      focusFirstInvalid(this.form, this.host);
      return;
    }
    // TODO: call POST /auth/login once the auth service is wired up.
  }
}

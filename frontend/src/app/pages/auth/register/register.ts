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

type Field = 'firstName' | 'lastName' | 'email' | 'password';

const REQUIRED: Record<Field, string> = {
  firstName: 'Enter your first name',
  lastName: 'Enter your last name',
  email: 'Enter your email address',
  password: 'Create a password',
};

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, NgIcon, GoogleButton, FieldMessage],
  viewProviders: [provideIcons({ heroEye, heroEyeSlash })],
  templateUrl: './register.html',
})
export class Register {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;

  protected readonly showPassword = signal(false);
  protected readonly capsLock = signal(false);
  private readonly submitted = signal(false);

  // Mirrors RegistrationRequestDto validation in user-service.
  protected readonly form = this.fb.group({
    firstName: ['', [notBlank, Validators.maxLength(100)]],
    lastName: ['', [notBlank, Validators.maxLength(100)]],
    email: ['', [notBlank, Validators.email, Validators.maxLength(255)]],
    password: ['', [notBlank, Validators.minLength(8), Validators.maxLength(100)]],
  });

  protected error(field: Field): string | null {
    const control = this.form.controls[field];
    if (!shouldShowError(control, this.submitted())) {
      return null;
    }
    if (control.hasError('required')) {
      return REQUIRED[field];
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
    this.submitted.set(true);
    if (this.form.invalid) {
      focusFirstInvalid(this.form, this.host);
      return;
    }
    // TODO: call POST /auth/register once the auth service is wired up.
  }
}

import { Component, DestroyRef, ElementRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroEye, heroEyeSlash } from '@ng-icons/heroicons/outline';
import { finalize } from 'rxjs';
import { genericErrorMessage, problemOf, statusOf } from '../../../core/http/problem-detail';
import { UsersService } from '../../../core/users/users.service';
import { FieldMessage } from '../../../shared/forms/field-message';
import { FormAlert } from '../../../shared/forms/form-alert';
import { focusFirstInvalid, notBlank, shouldShowError } from '../../../shared/forms/form-utils';
import {
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  PasswordRequirements,
} from '../../../shared/forms/password-requirements';
import { SettingsSection } from '../../../shared/settings-section/settings-section';
import { Spinner } from '../../../shared/spinner/spinner';

type Field = 'currentPassword' | 'newPassword' | 'confirmPassword';

const FIELDS: { id: Field; label: string; autocomplete: string; wide: boolean }[] = [
  {
    id: 'currentPassword',
    label: 'Current password',
    autocomplete: 'current-password',
    wide: true,
  },
  { id: 'newPassword', label: 'New password', autocomplete: 'new-password', wide: false },
  {
    id: 'confirmPassword',
    label: 'Confirm new password',
    autocomplete: 'new-password',
    wide: false,
  },
];

@Component({
  selector: 'app-password',
  imports: [
    ReactiveFormsModule,
    NgIcon,
    FieldMessage,
    FormAlert,
    PasswordRequirements,
    SettingsSection,
    Spinner,
  ],
  viewProviders: [provideIcons({ heroEye, heroEyeSlash })],
  templateUrl: './password.html',
})
export class Password {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly users = inject(UsersService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly currentPasswordRejected = signal(false);
  protected readonly fields = FIELDS;
  private readonly visible = signal<ReadonlySet<Field>>(new Set());
  private readonly submitted = signal(false);

  protected readonly form = this.fb.group({
    currentPassword: ['', notBlank],
    newPassword: [
      '',
      [
        notBlank,
        Validators.minLength(PASSWORD_MIN_LENGTH),
        Validators.maxLength(PASSWORD_MAX_LENGTH),
      ],
    ],
    confirmPassword: ['', notBlank],
  });

  protected readonly formValue = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed(inject(DestroyRef))).subscribe(() => {
      this.formError.set(null);
      this.currentPasswordRejected.set(false);
    });
  }

  protected isVisible(field: Field): boolean {
    return this.visible().has(field);
  }

  protected toggleVisible(field: Field): void {
    const next = new Set(this.visible());
    if (!next.delete(field)) {
      next.add(field);
    }
    this.visible.set(next);
  }

  protected isInvalid(field: Field): boolean {
    return (
      this.error(field) !== null || (field === 'currentPassword' && this.currentPasswordRejected())
    );
  }

  protected error(field: Field): string | null {
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
    return `Use between ${PASSWORD_MIN_LENGTH} and ${PASSWORD_MAX_LENGTH} characters`;
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
            this.formError.set(problemOf(error).detail ?? 'Current password is incorrect');
            this.currentPasswordRejected.set(true);
            this.host.querySelector<HTMLElement>('#currentPassword')?.focus();
            return;
          }
          this.formError.set(genericErrorMessage(error));
        },
      });
  }
}

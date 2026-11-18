import { Location } from '@angular/common';
import { Component, DestroyRef, ElementRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroEye, heroEyeSlash, heroLink } from '@ng-icons/heroicons/outline';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { genericErrorMessage, problemOf, statusOf } from '../../../core/http/problem-detail';
import { FieldMessage } from '../../../shared/forms/field-message';
import { FormAlert } from '../../../shared/forms/form-alert';
import { focusFirstInvalid, notBlank, shouldShowError } from '../../../shared/forms/form-utils';
import {
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  PasswordRequirements,
} from '../../../shared/forms/password-requirements';
import { Spinner } from '../../../shared/spinner/spinner';

type Field = 'newPassword' | 'confirmPassword';

const FIELDS: { id: Field; label: string }[] = [
  { id: 'newPassword', label: 'New password' },
  { id: 'confirmPassword', label: 'Confirm new password' },
];

@Component({
  selector: 'app-reset-password',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    NgIcon,
    FieldMessage,
    FormAlert,
    PasswordRequirements,
    Spinner,
  ],
  viewProviders: [provideIcons({ heroEye, heroEyeSlash, heroLink })],
  templateUrl: './reset-password.html',
})
export class ResetPassword {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly token = inject(ActivatedRoute).snapshot.queryParamMap.get('token');

  protected readonly fields = FIELDS;
  protected readonly linkInvalid = signal(!this.token);
  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  private readonly visible = signal<ReadonlySet<Field>>(new Set());
  private readonly submitted = signal(false);

  protected readonly form = this.fb.group({
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
    if (this.token) {
      inject(Location).replaceState('/reset-password');
    }

    this.form.valueChanges
      .pipe(takeUntilDestroyed(inject(DestroyRef)))
      .subscribe(() => this.formError.set(null));
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
      return field === 'newPassword' ? 'Enter a new password' : 'Enter the new password again';
    }
    return `Use between ${PASSWORD_MIN_LENGTH} and ${PASSWORD_MAX_LENGTH} characters`;
  }

  protected onSubmit(): void {
    if (this.submitting() || !this.token) {
      return;
    }
    this.submitted.set(true);
    const { newPassword, confirmPassword } = this.form.getRawValue();
    if (this.form.invalid || newPassword !== confirmPassword) {
      if (this.form.valid) {
        this.host.querySelector<HTMLElement>('#confirmPassword')?.focus();
      } else {
        focusFirstInvalid(this.form, this.host);
      }
      return;
    }

    this.submitting.set(true);
    this.auth
      .resetPassword(this.token, newPassword)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => this.router.navigateByUrl('/login', { state: { notice: 'password-reset' } }),
        error: (error) => this.handleError(error),
      });
  }

  private handleError(error: unknown): void {
    const problem = problemOf(error);
    if (statusOf(error) === 400 && problem.errors) {
      this.formError.set(
        Object.values(problem.errors)
          .map((messages) => messages[0])
          .join(' '),
      );
      return;
    }
    if (statusOf(error) === 400) {
      this.linkInvalid.set(true);
      return;
    }
    this.formError.set(genericErrorMessage(error));
  }
}

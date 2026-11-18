import { Component, DestroyRef, ElementRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, map, startWith } from 'rxjs';
import { SessionUser } from '../../../core/auth/auth.models';
import { AuthService } from '../../../core/auth/auth.service';
import { genericErrorMessage, problemOf, statusOf } from '../../../core/http/problem-detail';
import { UpdateProfileRequest } from '../../../core/users/users.models';
import { UsersService } from '../../../core/users/users.service';
import { FieldMessage } from '../../../shared/forms/field-message';
import { FormAlert } from '../../../shared/forms/form-alert';
import { focusFirstInvalid, notBlank, shouldShowError } from '../../../shared/forms/form-utils';
import { SettingsSection } from '../../../shared/settings-section/settings-section';
import { Spinner } from '../../../shared/spinner/spinner';
import { ToastService } from '../../../shared/toast/toast.service';

type Field = 'firstName' | 'lastName' | 'email' | 'phoneNumber' | 'currentPassword';

const PHONE_PATTERN = /^\+?[0-9]{7,15}$/;

@Component({
  selector: 'app-profile',
  imports: [ReactiveFormsModule, FieldMessage, FormAlert, SettingsSection, Spinner],
  templateUrl: './profile.html',
})
export class Profile {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly auth = inject(AuthService);
  private readonly users = inject(UsersService);
  private readonly toast = inject(ToastService);

  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  private readonly rejectedFields = signal<ReadonlySet<Field>>(new Set());
  private readonly submitted = signal(false);

  protected readonly form = this.fb.group({
    firstName: ['', [notBlank, Validators.maxLength(100)]],
    lastName: ['', [notBlank, Validators.maxLength(100)]],
    email: ['', [notBlank, Validators.email, Validators.maxLength(255)]],
    phoneNumber: ['', Validators.pattern(PHONE_PATTERN)],
    currentPassword: [''],
  });

  private readonly email = toSignal(
    this.form.controls.email.valueChanges.pipe(
      startWith(this.form.controls.email.value),
      map((value) => value.trim()),
    ),
    { requireSync: true },
  );

  private readonly formValue = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });

  protected readonly hasChanges = computed(() => {
    this.formValue();
    return Object.keys(this.changes()).length > 0;
  });

  protected readonly emailChanged = computed(
    () => this.email() !== (this.auth.user()?.email ?? ''),
  );

  constructor() {
    const user = this.auth.user();
    if (user) {
      this.fill(user);
    }

    this.form.valueChanges.pipe(takeUntilDestroyed(inject(DestroyRef))).subscribe(() => {
      this.formError.set(null);
      this.rejectedFields.set(new Set());
    });
  }

  protected isInvalid(field: Field): boolean {
    return this.error(field) !== null || this.rejectedFields().has(field);
  }

  protected error(field: Field): string | null {
    const control = this.form.controls[field];
    if (field === 'currentPassword') {
      return this.submitted() && this.emailChanged() && !control.value
        ? 'Enter your current password to change your email'
        : null;
    }
    if (!shouldShowError(control, this.submitted())) {
      return null;
    }
    if (control.hasError('required')) {
      return field === 'email'
        ? 'Enter your email address'
        : `Enter your ${field === 'firstName' ? 'first' : 'last'} name`;
    }
    if (control.hasError('maxlength')) {
      return 'This is too long';
    }
    if (control.hasError('email')) {
      return 'Enter a valid email address';
    }
    return 'Use 7 to 15 digits, optionally starting with +';
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
    if (this.emailChanged() && !this.form.controls.currentPassword.value) {
      this.host.querySelector<HTMLElement>('#currentPassword')?.focus();
      return;
    }

    const request = this.changes();
    if (Object.keys(request).length === 0) {
      return;
    }

    this.submitting.set(true);
    this.users
      .updateProfile(request)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => {
          this.fill(this.auth.user()!);
          this.submitted.set(false);
          this.toast.success('Profile saved.');
        },
        error: (error) => this.handleError(error),
      });
  }

  protected discard(): void {
    this.fill(this.auth.user()!);
    this.submitted.set(false);
  }

  private changes(): UpdateProfileRequest {
    const user = this.auth.user()!;
    const value = this.form.getRawValue();
    const request: UpdateProfileRequest = {};

    if (value.firstName.trim() !== user.firstName) {
      request.firstName = value.firstName.trim();
    }
    if (value.lastName.trim() !== user.lastName) {
      request.lastName = value.lastName.trim();
    }
    if (value.phoneNumber && value.phoneNumber !== (user.phoneNumber ?? '')) {
      request.phoneNumber = value.phoneNumber;
    }
    if (this.emailChanged()) {
      request.email = value.email.trim();
      request.currentPassword = value.currentPassword;
    }
    return request;
  }

  private fill(user: SessionUser): void {
    this.form.reset(
      {
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.email,
        phoneNumber: user.phoneNumber ?? '',
        currentPassword: '',
      },
      { emitEvent: true },
    );
  }

  private handleError(error: unknown): void {
    const problem = problemOf(error);
    const status = statusOf(error);

    if (status === 400 && problem.errors) {
      const entries = Object.entries(problem.errors);
      this.reject(
        entries.map(([field]) => field as Field),
        entries.map(([, messages]) => messages[0]).join(' '),
      );
      return;
    }
    if (status === 400) {
      this.reject(['currentPassword'], problem.detail ?? 'Current password is incorrect');
      return;
    }
    if (status === 409) {
      const field: Field = problem.title?.toLowerCase().includes('phone') ? 'phoneNumber' : 'email';
      this.reject([field], problem.detail ?? 'This is already in use');
      return;
    }
    this.formError.set(genericErrorMessage(error));
  }

  private reject(fields: Field[], message: string): void {
    this.formError.set(message);
    this.rejectedFields.set(new Set(fields));
  }
}

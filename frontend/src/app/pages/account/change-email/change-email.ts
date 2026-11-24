import {
  Component,
  DestroyRef,
  ElementRef,
  Injector,
  afterNextRender,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroEnvelope } from '@ng-icons/heroicons/outline';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { genericErrorMessage, problemOf, statusOf } from '../../../core/http/problem-detail';
import { UsersService } from '../../../core/users/users.service';
import { FieldMessage } from '../../../shared/forms/field-message';
import { FormAlert } from '../../../shared/forms/form-alert';
import { notBlank, shouldShowError } from '../../../shared/forms/form-utils';
import { SettingsSection } from '../../../shared/settings-section/settings-section';
import { Spinner } from '../../../shared/spinner/spinner';

interface SentLink {
  currentEmail: string;
  newEmail: string;
}

@Component({
  selector: 'app-change-email',
  imports: [ReactiveFormsModule, NgIcon, FieldMessage, FormAlert, SettingsSection, Spinner],
  viewProviders: [provideIcons({ heroEnvelope })],
  templateUrl: './change-email.html',
})
export class ChangeEmail {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly auth = inject(AuthService);
  private readonly users = inject(UsersService);
  private readonly injector = inject(Injector);

  protected readonly currentEmail = computed(() => this.auth.user()?.email ?? '');
  protected readonly expanded = signal(false);
  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly rejected = signal(false);
  protected readonly sent = signal<SentLink | null>(null);
  private readonly submitted = signal(false);

  protected readonly form = this.fb.group({
    newEmail: ['', [notBlank, Validators.email, Validators.maxLength(255)]],
  });

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed(inject(DestroyRef))).subscribe(() => {
      this.formError.set(null);
      this.rejected.set(false);
    });
  }

  protected expand(): void {
    this.expanded.set(true);
    this.focusAfterRender('#newEmail');
  }

  protected close(): void {
    this.form.reset();
    this.submitted.set(false);
    this.sent.set(null);
    this.expanded.set(false);
    this.focusAfterRender('#change-email-toggle');
  }

  protected error(): string | null {
    const control = this.form.controls.newEmail;
    if (this.submitted() && this.isCurrentEmail(control.value)) {
      return 'This is already your email';
    }
    if (!shouldShowError(control, this.submitted())) {
      return null;
    }
    if (control.hasError('required')) {
      return 'Enter your new email address';
    }
    if (control.hasError('maxlength')) {
      return 'This is too long';
    }
    return 'Enter a valid email address';
  }

  protected onSubmit(): void {
    if (this.submitting()) {
      return;
    }
    this.submitted.set(true);
    const newEmail = this.form.controls.newEmail.value.trim();
    if (this.form.invalid || this.isCurrentEmail(newEmail)) {
      this.host.querySelector<HTMLElement>('#newEmail')?.focus();
      return;
    }

    const currentEmail = this.currentEmail();
    this.submitting.set(true);
    this.users
      .requestEmailChange(newEmail)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => this.sent.set({ currentEmail, newEmail }),
        error: (error) => this.handleError(error),
      });
  }

  private isCurrentEmail(value: string): boolean {
    return value.trim().toLowerCase() === this.currentEmail().toLowerCase();
  }

  private handleError(error: unknown): void {
    const problem = problemOf(error);
    const status = statusOf(error);
    if (status === 409 || (status === 400 && problem.errors)) {
      this.formError.set(
        problem.errors
          ? Object.values(problem.errors)
              .map((messages) => messages[0])
              .join(' ')
          : (problem.detail ?? 'An account with this email already exists'),
      );
      this.rejected.set(true);
      this.host.querySelector<HTMLElement>('#newEmail')?.focus();
      return;
    }
    this.formError.set(genericErrorMessage(error));
  }

  private focusAfterRender(selector: string): void {
    afterNextRender(() => this.host.querySelector<HTMLElement>(selector)?.focus(), {
      injector: this.injector,
    });
  }
}

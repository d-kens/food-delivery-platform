import {
  Component,
  DestroyRef,
  ElementRef,
  Injector,
  afterNextRender,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroExclamationTriangle } from '@ng-icons/heroicons/outline';
import { finalize } from 'rxjs';
import { genericErrorMessage, problemOf, statusOf } from '../../../core/http/problem-detail';
import { UsersService } from '../../../core/users/users.service';
import { FieldMessage } from '../../../shared/forms/field-message';
import { FormAlert } from '../../../shared/forms/form-alert';
import { focusFirstInvalid, notBlank, shouldShowError } from '../../../shared/forms/form-utils';
import { SettingsSection } from '../../../shared/settings-section/settings-section';
import { Spinner } from '../../../shared/spinner/spinner';

@Component({
  selector: 'app-delete-account',
  imports: [ReactiveFormsModule, NgIcon, FieldMessage, FormAlert, SettingsSection, Spinner],
  viewProviders: [provideIcons({ heroExclamationTriangle })],
  templateUrl: './delete-account.html',
})
export class DeleteAccount {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly users = inject(UsersService);
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);

  protected readonly expanded = signal(false);
  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly passwordRejected = signal(false);
  private readonly submitted = signal(false);

  protected readonly form = this.fb.group({
    deletePassword: ['', notBlank],
    confirm: [false, Validators.requiredTrue],
  });

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed(inject(DestroyRef))).subscribe(() => {
      this.formError.set(null);
      this.passwordRejected.set(false);
    });
  }

  protected expand(): void {
    this.expanded.set(true);
    this.focusAfterRender('#deletePassword');
  }

  protected cancel(): void {
    this.form.reset();
    this.submitted.set(false);
    this.expanded.set(false);
    this.focusAfterRender('#delete-account-toggle');
  }

  protected passwordMessage(): string | null {
    return shouldShowError(this.form.controls.deletePassword, this.submitted())
      ? 'Enter your current password'
      : null;
  }

  protected confirmMessage(): string | null {
    return shouldShowError(this.form.controls.confirm, this.submitted())
      ? 'Tick the box to confirm you understand'
      : null;
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
    this.users
      .deleteAccount(this.form.controls.deletePassword.value)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => this.router.navigateByUrl('/login', { state: { notice: 'account-deleted' } }),
        error: (error) => {
          if (statusOf(error) === 400 && !problemOf(error).errors) {
            this.formError.set(problemOf(error).detail ?? 'Current password is incorrect');
            this.passwordRejected.set(true);
            this.host.querySelector<HTMLElement>('#deletePassword')?.focus();
            return;
          }
          this.formError.set(genericErrorMessage(error));
        },
      });
  }

  private focusAfterRender(selector: string): void {
    afterNextRender(() => this.host.querySelector<HTMLElement>(selector)?.focus(), {
      injector: this.injector,
    });
  }
}

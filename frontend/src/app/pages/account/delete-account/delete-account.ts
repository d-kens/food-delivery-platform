import { Component, ElementRef, Injector, afterNextRender, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroEnvelope, heroExclamationTriangle } from '@ng-icons/heroicons/outline';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { genericErrorMessage } from '../../../core/http/problem-detail';
import { UsersService } from '../../../core/users/users.service';
import { FieldMessage } from '../../../shared/forms/field-message';
import { FormAlert } from '../../../shared/forms/form-alert';
import { focusFirstInvalid, shouldShowError } from '../../../shared/forms/form-utils';
import { SettingsSection } from '../../../shared/settings-section/settings-section';
import { Spinner } from '../../../shared/spinner/spinner';

@Component({
  selector: 'app-delete-account',
  imports: [ReactiveFormsModule, NgIcon, FieldMessage, FormAlert, SettingsSection, Spinner],
  viewProviders: [provideIcons({ heroEnvelope, heroExclamationTriangle })],
  templateUrl: './delete-account.html',
})
export class DeleteAccount {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly auth = inject(AuthService);
  private readonly users = inject(UsersService);
  private readonly injector = inject(Injector);

  protected readonly expanded = signal(false);
  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly sentTo = signal<string | null>(null);
  private readonly submitted = signal(false);

  protected readonly form = this.fb.group({
    confirm: [false, Validators.requiredTrue],
  });

  protected expand(): void {
    this.expanded.set(true);
    this.focusAfterRender('#confirm');
  }

  protected cancel(): void {
    this.form.reset();
    this.submitted.set(false);
    this.formError.set(null);
    this.sentTo.set(null);
    this.expanded.set(false);
    this.focusAfterRender('#delete-account-toggle');
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

    this.formError.set(null);
    this.submitting.set(true);
    this.users
      .requestAccountDeletion()
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => this.sentTo.set(this.auth.user()?.email ?? 'your email'),
        error: (error) => this.formError.set(genericErrorMessage(error)),
      });
  }

  private focusAfterRender(selector: string): void {
    afterNextRender(() => this.host.querySelector<HTMLElement>(selector)?.focus(), {
      injector: this.injector,
    });
  }
}

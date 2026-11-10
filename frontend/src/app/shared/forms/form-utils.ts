import { AbstractControl, FormGroup, ValidationErrors } from '@angular/forms';

export function notBlank(control: AbstractControl<string>): ValidationErrors | null {
  return control.value?.trim() ? null : { required: true };
}

export function shouldShowError(control: AbstractControl, submitted: boolean): boolean {
  return control.invalid && (submitted || control.touched);
}

export function focusFirstInvalid(form: FormGroup, host: HTMLElement): void {
  const name = Object.keys(form.controls).find((key) => form.controls[key].invalid);
  if (name) {
    host.querySelector<HTMLElement>(`#${name}`)?.focus();
  }
}

export function isCapsLockOn(event: KeyboardEvent): boolean {
  return event.getModifierState?.('CapsLock') ?? false;
}

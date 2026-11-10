import { AbstractControl, FormGroup, ValidationErrors } from '@angular/forms';

/** Like Validators.required, but also rejects whitespace-only input (matches @NotBlank). */
export function notBlank(control: AbstractControl<string>): ValidationErrors | null {
  return control.value?.trim() ? null : { required: true };
}

/** Errors show once the user has left a field (even untouched), or after a submit attempt. */
export function shouldShowError(control: AbstractControl, submitted: boolean): boolean {
  return control.invalid && (submitted || control.touched);
}

/** Focuses the input of the first invalid control, in form order. */
export function focusFirstInvalid(form: FormGroup, host: HTMLElement): void {
  const name = Object.keys(form.controls).find((key) => form.controls[key].invalid);
  if (name) {
    host.querySelector<HTMLElement>(`#${name}`)?.focus();
  }
}

/** True when Caps Lock is on, for the password warning. */
export function isCapsLockOn(event: KeyboardEvent): boolean {
  return event.getModifierState?.('CapsLock') ?? false;
}

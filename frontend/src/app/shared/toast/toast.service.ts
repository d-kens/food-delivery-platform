import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  tone: 'success' | 'error';
  text: string;
}

const SUCCESS_DURATION_MS = 4000;

@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 0;
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>>();

  readonly toasts = signal<Toast[]>([]);

  success(text: string): void {
    const id = this.add('success', text);
    this.timers.set(
      id,
      setTimeout(() => this.dismiss(id), SUCCESS_DURATION_MS),
    );
  }

  error(text: string): void {
    this.add('error', text);
  }

  dismiss(id: number): void {
    clearTimeout(this.timers.get(id));
    this.timers.delete(id);
    this.toasts.update((toasts) => toasts.filter((toast) => toast.id !== id));
  }

  private add(tone: Toast['tone'], text: string): number {
    const id = ++this.nextId;
    this.toasts.update((toasts) => [...toasts, { id, tone, text }]);
    return id;
  }
}

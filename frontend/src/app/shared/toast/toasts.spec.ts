import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToastService } from './toast.service';
import { Toasts } from './toasts';

describe('Toasts', () => {
  let fixture: ComponentFixture<Toasts>;
  let compiled: HTMLElement;
  let service: ToastService;

  beforeEach(async () => {
    fixture = TestBed.createComponent(Toasts);
    compiled = fixture.nativeElement as HTMLElement;
    service = TestBed.inject(ToastService);
    await fixture.whenStable();
  });

  it('should announce success as status and errors as alerts', async () => {
    service.success('Profile saved.');
    service.error('Could not save.');
    await fixture.whenStable();

    expect(compiled.querySelector('[role="status"]')?.textContent).toContain('Profile saved.');
    expect(compiled.querySelector('[role="alert"]')?.textContent).toContain('Could not save.');
  });

  it('should remove a toast when its dismiss button is clicked', async () => {
    service.error('Could not save.');
    await fixture.whenStable();

    compiled.querySelector<HTMLButtonElement>('button[aria-label="Dismiss"]')!.click();
    await fixture.whenStable();

    expect(compiled.querySelector('[role="alert"]')).toBeNull();
  });
});

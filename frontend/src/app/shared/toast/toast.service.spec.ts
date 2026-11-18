import { TestBed } from '@angular/core/testing';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    vi.useFakeTimers();
    service = TestBed.inject(ToastService);
  });

  afterEach(() => vi.useRealTimers());

  it('should hide a success toast after four seconds', () => {
    service.success('Profile saved.');
    expect(service.toasts()).toEqual([
      expect.objectContaining({ tone: 'success', text: 'Profile saved.' }),
    ]);

    vi.advanceTimersByTime(4000);

    expect(service.toasts()).toEqual([]);
  });

  it('should keep an error toast until it is dismissed', () => {
    service.error('Could not save.');
    vi.advanceTimersByTime(60_000);

    const [toast] = service.toasts();
    expect(toast).toEqual(expect.objectContaining({ tone: 'error', text: 'Could not save.' }));

    service.dismiss(toast.id);
    expect(service.toasts()).toEqual([]);
  });

  it('should show several toasts in the order they were added', () => {
    service.success('First');
    service.error('Second');

    expect(service.toasts().map((toast) => toast.text)).toEqual(['First', 'Second']);
  });
});

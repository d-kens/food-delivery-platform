import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Header } from './header';

describe('Header mobile menu', () => {
  let fixture: ComponentFixture<Header>;
  let compiled: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Header],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(Header);
    await fixture.whenStable();
    compiled = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => (document.body.style.overflow = ''));

  const toggle = () => compiled.querySelector<HTMLButtonElement>('button[aria-controls="mobile-menu"]')!;
  const menu = () => compiled.querySelector('#mobile-menu');

  const open = async () => {
    toggle().click();
    await fixture.whenStable();
  };

  it('should open a full-screen menu with links, partner cards and account actions', async () => {
    await open();

    expect(toggle().getAttribute('aria-expanded')).toBe('true');
    expect(menu()?.textContent).toContain('Restaurants');
    expect(menu()?.textContent).toContain('Add your restaurant');
    expect(menu()?.textContent).toContain('Become a rider');
    expect(menu()?.textContent).toContain('Sign up');
    expect(menu()?.textContent).toContain('Log in');
  });

  it('should stop the page behind from scrolling while open', async () => {
    await open();
    expect(document.body.style.overflow).toBe('hidden');

    toggle().click();
    await fixture.whenStable();
    expect(document.body.style.overflow).toBe('');
  });

  it('should close on Escape and return focus to the menu button', async () => {
    await open();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    await fixture.whenStable();

    expect(menu()).toBeNull();
    expect(toggle().getAttribute('aria-expanded')).toBe('false');
    expect(document.activeElement).toBe(toggle());
  });

  it('should move focus into the menu when it opens, without focusing the address field', async () => {
    document.body.appendChild(compiled);
    await open();

    expect(document.activeElement).toBe(menu());
    expect(document.activeElement?.id).not.toBe('mobile-address');
    compiled.remove();
  });

  it('should keep Tab focus inside the menu', async () => {
    document.body.appendChild(compiled);
    await open();
    const focusable = menu()!.querySelectorAll<HTMLElement>('a[href], button, input');
    focusable[focusable.length - 1].focus();

    const tab = new KeyboardEvent('keydown', { key: 'Tab', cancelable: true });
    document.dispatchEvent(tab);

    expect(tab.defaultPrevented).toBe(true);
    expect(document.activeElement).toBe(toggle());
    compiled.remove();
  });

  it('should offer an address search, help and partner links', async () => {
    await open();

    expect(menu()?.querySelector('#mobile-address')).toBeTruthy();
    expect(menu()?.textContent).toContain('Help centre');
    expect(menu()?.textContent).not.toContain('Partners');
  });
});

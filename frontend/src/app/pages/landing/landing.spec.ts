import { ApplicationRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Landing } from './landing';

describe('Landing', () => {
  let compiled: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Landing],
      providers: [provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(Landing);
    await fixture.whenStable();
    compiled = fixture.nativeElement as HTMLElement;
  });

  it('should render the hero heading and address search', () => {
    expect(compiled.querySelector('h1')?.textContent).toContain('Good food, delivered to your door.');
    expect(compiled.querySelector('input#address')).toBeTruthy();
  });

  it('should render a card for every popular restaurant', () => {
    const cards = compiled.querySelectorAll('#restaurants article');

    expect(cards.length).toBe(8);
    expect(cards[0].textContent).toContain('Urban Grill House');
  });

  it('should render the how it works steps and partner calls to action', () => {
    expect(compiled.querySelectorAll('#how-it-works li').length).toBe(3);
    expect(compiled.querySelector('#partners')?.textContent).toContain('Add your restaurant');
    expect(compiled.querySelector('#partners')?.textContent).toContain('Start riding');
  });

  it('should toggle a restaurant as favourite', async () => {
    const button = compiled.querySelector<HTMLButtonElement>('#restaurants article button')!;
    expect(button.getAttribute('aria-pressed')).toBe('false');

    button.click();
    await TestBed.inject(ApplicationRef).whenStable();
    expect(button.getAttribute('aria-pressed')).toBe('true');
    expect(button.getAttribute('aria-label')).toBe('Remove Urban Grill House from favourites');

    button.click();
    await TestBed.inject(ApplicationRef).whenStable();
    expect(button.getAttribute('aria-pressed')).toBe('false');
  });
});

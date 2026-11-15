import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { THEME_STORAGE_KEY, ThemeStore, optionOf } from './theme-store';

describe('ThemeStore', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    TestBed.resetTestingModule();
  });

  it('starts with Sage Ledger', () => {
    expect(TestBed.inject(ThemeStore).current).toBe('sage');
  });

  it('remembers the choice for the next visit', () => {
    TestBed.inject(ThemeStore).select('midnight');

    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('midnight');
    TestBed.resetTestingModule();
    expect(TestBed.inject(ThemeStore).current).toBe('midnight');
  });

  it('ignores a stored value that is not a theme', () => {
    localStorage.setItem(THEME_STORAGE_KEY, 'neon');
    expect(TestBed.inject(ThemeStore).current).toBe('sage');
  });

  it('puts the theme on the page only while the shell is attached', () => {
    const store = TestBed.inject(ThemeStore);
    store.select('cobalt');
    expect(document.documentElement.getAttribute('data-theme')).toBeNull();

    store.attach();
    expect(document.documentElement.getAttribute('data-theme')).toBe('cobalt');
    store.select('graphite');
    expect(document.documentElement.getAttribute('data-theme')).toBe('graphite');

    store.detach();
    expect(document.documentElement.getAttribute('data-theme')).toBeNull();
  });

  it('gives Graphite & Lime the top navigation', async () => {
    const store = TestBed.inject(ThemeStore);
    store.select('graphite');
    expect((await firstValueFrom(store.option$)).layout).toBe('top');
    expect(optionOf('sage').layout).toBe('side');
  });
});

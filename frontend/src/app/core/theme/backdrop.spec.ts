import { TestBed } from '@angular/core/testing';
import { BehaviorSubject, firstValueFrom } from 'rxjs';
import { AuthStore } from '../auth/auth-store';
import { Session } from '../auth/models';
import { BackdropStore, DAY_PHOTOS, NIGHT_PHOTOS, backdropFor, sessionSeed } from './backdrop';
import { ThemeStore } from './theme-store';

function session(token: string): Session {
  return {
    accessToken: token,
    expiresAt: new Date(Date.now() + 3600_000).toISOString(),
    user: {
      id: 7,
      email: 'lena.fischer@uni.example',
      firstName: 'Lena',
      lastName: 'Fischer',
      role: 'STUDENT',
    },
  };
}

describe('backdrop photo', () => {
  it('takes light themes from the day photos and dark themes from the night photos', () => {
    expect(backdropFor(7, false)).toBe(DAY_PHOTOS[7 % DAY_PHOTOS.length]);
    expect(backdropFor(7, true)).toBe(NIGHT_PHOTOS[7 % NIGHT_PHOTOS.length]);
    expect(NIGHT_PHOTOS.length).toBe(3);
    expect(DAY_PHOTOS.length).toBe(5);
  });

  it('is the same for one sign-in and new at the next', () => {
    expect(sessionSeed('token-of-this-sign-in')).toBe(sessionSeed('token-of-this-sign-in'));
    expect(sessionSeed('token-of-this-sign-in')).not.toBe(sessionSeed('token-of-the-next-sign-in'));
    expect(sessionSeed('anything')).toBeGreaterThanOrEqual(0);
  });

  it('spreads sign-ins over the whole pool', () => {
    const seen = new Set(
      Array.from({ length: 60 }, (_, i) =>
        backdropFor(sessionSeed('header.payload.signature' + i), false),
      ),
    );
    expect(seen.size).toBe(DAY_PHOTOS.length);
  });
});

describe('BackdropStore', () => {
  const sessions = new BehaviorSubject<Session | null>(null);

  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [{ provide: AuthStore, useValue: { session$: sessions } }],
    });
  });

  it('shows no photo when nobody is signed in', async () => {
    sessions.next(null);
    expect(await firstValueFrom(TestBed.inject(BackdropStore).photo$)).toBeNull();
  });

  it('keeps the same number for the session and follows the theme to the matching pool', async () => {
    const seed = sessionSeed('jwt-a');
    sessions.next(session('jwt-a'));
    const store = TestBed.inject(BackdropStore);
    const themes = TestBed.inject(ThemeStore);

    themes.select('sage');
    expect(await firstValueFrom(store.photo$)).toBe(DAY_PHOTOS[seed % DAY_PHOTOS.length]);
    themes.select('midnight');
    expect(await firstValueFrom(store.photo$)).toBe(NIGHT_PHOTOS[seed % NIGHT_PHOTOS.length]);
    themes.select('cobalt');
    expect(await firstValueFrom(store.photo$)).toBe(DAY_PHOTOS[seed % DAY_PHOTOS.length]);
    themes.select('graphite');
    expect(await firstValueFrom(store.photo$)).toBe(NIGHT_PHOTOS[seed % NIGHT_PHOTOS.length]);
  });

  it('draws again at the next sign-in', async () => {
    const store = TestBed.inject(BackdropStore);
    TestBed.inject(ThemeStore).select('sage');
    sessions.next(session('jwt-first'));
    const first = await firstValueFrom(store.photo$);
    sessions.next(session('jwt-second'));
    expect(await firstValueFrom(store.photo$)).toBe(backdropFor(sessionSeed('jwt-second'), false));
    expect(first).toBe(backdropFor(sessionSeed('jwt-first'), false));
  });
});

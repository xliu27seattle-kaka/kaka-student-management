import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { AuthApi } from './auth-api';
import { AuthStore, SESSION_STORAGE_KEY } from './auth-store';
import { Session } from './models';

const inAnHour = () => new Date(Date.now() + 60 * 60 * 1000).toISOString();

function session(expiresAt = inAnHour()): Session {
  return {
    accessToken: 'jwt-value',
    expiresAt,
    user: {
      id: 7,
      email: 'lena.fischer@uni.example',
      firstName: 'Lena',
      lastName: 'Fischer',
      role: 'STUDENT',
    },
  };
}

function createStore(loginResult: Session = session()): AuthStore {
  TestBed.configureTestingModule({
    providers: [{ provide: AuthApi, useValue: { login: () => of(loginResult) } }],
  });
  return TestBed.inject(AuthStore);
}

describe('AuthStore', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('starts signed out when nothing is stored', async () => {
    const store = createStore();

    expect(store.snapshot).toBeNull();
    expect(await firstValueFrom(store.session$)).toBeNull();
  });

  it('keeps the session after a successful login', async () => {
    const store = createStore();

    await firstValueFrom(store.login('lena.fischer@uni.example', 'Passw0rd1'));

    expect(store.snapshot?.user.role).toBe('STUDENT');
    expect(store.token).toBe('jwt-value');
    expect(await firstValueFrom(store.role$)).toBe('STUDENT');
    expect(JSON.parse(localStorage.getItem(SESSION_STORAGE_KEY)!).accessToken).toBe('jwt-value');
  });

  it('forgets the session on logout', async () => {
    const store = createStore();
    await firstValueFrom(store.login('lena.fischer@uni.example', 'Passw0rd1'));

    store.logout();

    expect(store.snapshot).toBeNull();
    expect(store.token).toBeNull();
    expect(localStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
  });

  it('restores a stored session that has not expired', () => {
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session()));

    const store = createStore();

    expect(store.snapshot?.user.email).toBe('lena.fischer@uni.example');
  });

  it('ignores and removes a stored session that has expired', () => {
    const yesterday = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session(yesterday)));

    const store = createStore();

    expect(store.snapshot).toBeNull();
    expect(localStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
  });

  it('ignores stored text that is not a session', () => {
    localStorage.setItem(SESSION_STORAGE_KEY, '{not json');

    expect(createStore().snapshot).toBeNull();
  });
});

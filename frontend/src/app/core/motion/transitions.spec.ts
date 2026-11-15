import { tabDirection, transitionKind } from './transitions';

describe('tabDirection', () => {
  const tabs = [{ id: 'profile' }, { id: 'enrollments' }, { id: 'transcript' }];

  it('moves forward to a tab further right and back to one further left', () => {
    expect(tabDirection(tabs, 'profile', 'transcript')).toBe('forward');
    expect(tabDirection(tabs, 'transcript', 'enrollments')).toBe('back');
  });

  it('does not move when the tab is the same or unknown', () => {
    expect(tabDirection(tabs, 'profile', 'profile')).toBe('none');
    expect(tabDirection(tabs, 'profile', 'missing')).toBe('none');
  });
});

describe('transitionKind', () => {
  it('enters the app from the sign-in or registration page', () => {
    expect(transitionKind('login', 'student')).toBe('enter');
    expect(transitionKind('register', 'admin')).toBe('enter');
  });

  it('leaves the app for the sign-in page', () => {
    expect(transitionKind('professor', 'login')).toBe('leave');
  });

  it('moves between pages inside one area quietly', () => {
    expect(transitionKind('admin', 'admin')).toBe('page');
  });

  it('switches between sign-in and registration without the app transition', () => {
    expect(transitionKind('login', 'register')).toBe('none');
    expect(transitionKind(null, 'login')).toBe('none');
  });
});

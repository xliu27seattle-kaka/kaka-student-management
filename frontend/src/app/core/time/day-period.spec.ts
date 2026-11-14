import { greetingOf, periodOf } from './day-period';

describe('periodOf', () => {
  it('is dawn from 5 until 8', () => {
    expect(periodOf(4)).toBe('night');
    expect(periodOf(5)).toBe('dawn');
    expect(periodOf(7)).toBe('dawn');
  });

  it('is day from 8 until 17', () => {
    expect(periodOf(8)).toBe('day');
    expect(periodOf(16)).toBe('day');
  });

  it('is dusk from 17 until 20', () => {
    expect(periodOf(17)).toBe('dusk');
    expect(periodOf(19)).toBe('dusk');
  });

  it('is night from 20 until 5', () => {
    expect(periodOf(20)).toBe('night');
    expect(periodOf(0)).toBe('night');
  });
});

describe('greetingOf', () => {
  it('says good morning from 5 until noon', () => {
    expect(greetingOf(5)).toBe('Good morning.');
    expect(greetingOf(11)).toBe('Good morning.');
  });

  it('says good afternoon from noon until 18', () => {
    expect(greetingOf(12)).toBe('Good afternoon.');
    expect(greetingOf(17)).toBe('Good afternoon.');
  });

  it('says good evening otherwise', () => {
    expect(greetingOf(18)).toBe('Good evening.');
    expect(greetingOf(3)).toBe('Good evening.');
  });
});

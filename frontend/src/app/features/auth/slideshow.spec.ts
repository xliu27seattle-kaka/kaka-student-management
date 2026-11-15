import { advance, firstSlide, wrap } from './slideshow';

describe('wrap', () => {
  it('keeps an index inside the list, in both directions', () => {
    expect(wrap(5, 5)).toBe(0);
    expect(wrap(-1, 5)).toBe(4);
    expect(wrap(3, 5)).toBe(3);
  });
});

describe('firstSlide', () => {
  it('shows the first photograph and nothing behind it', () => {
    expect(firstSlide()).toEqual({ current: 0, previous: null });
  });
});

describe('advance', () => {
  it('keeps the photograph that was in front as the one behind the new one', () => {
    expect(advance({ current: 0, previous: null }, 1)).toEqual({ current: 1, previous: 0 });
    expect(advance({ current: 1, previous: 0 }, 3)).toEqual({ current: 3, previous: 1 });
  });

  it('changes nothing when the photograph in front is chosen again', () => {
    const slide = { current: 2, previous: 1 };
    expect(advance(slide, 2)).toBe(slide);
  });
});

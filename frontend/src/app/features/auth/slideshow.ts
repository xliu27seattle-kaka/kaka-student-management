/**
 * Which photograph of the entry page is in front, and which one it is fading in over.
 * The one behind stays fully visible until the new one has covered it, so the page never
 * darkens halfway through a change.
 */
export interface Slide {
  current: number;
  previous: number | null;
}

/** Keeps an index inside a list of `count` items, so "next" after the last is the first. */
export function wrap(index: number, count: number): number {
  return ((index % count) + count) % count;
}

export function firstSlide(): Slide {
  return { current: 0, previous: null };
}

/** The slide after choosing photograph `next`; the same object when it is already in front. */
export function advance(slide: Slide, next: number): Slide {
  return next === slide.current ? slide : { current: next, previous: slide.current };
}

/**
 * Resolves once the photograph is downloaded and decoded, so the cross-fade starts with a
 * finished picture. A photograph that fails to load is shown anyway (the page stays usable).
 */
export function preload(src: string): Promise<void> {
  const image = new Image();
  image.src = src;
  if (typeof image.decode !== 'function') {
    return Promise.resolve();
  }
  return image.decode().catch(() => undefined);
}

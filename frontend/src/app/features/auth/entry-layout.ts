import { AsyncPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  BehaviorSubject,
  distinctUntilChanged,
  filter,
  from,
  map,
  Subject,
  switchMap,
  timer,
} from 'rxjs';
import { greetingOf, periodOf } from '../../core/time/day-period';
import { PHOTO_SCENES } from './photo-scenes';
import { ROLE_INTROS } from './role-intros';
import { advance, firstSlide, preload, wrap } from './slideshow';

/** How long each photograph stays in front. The bar of the current one fills up in this time. */
const ROTATE_EVERY_MS = 12_000;

/**
 * The frame shared by the login and registration pages: a full-screen photograph that slowly
 * zooms and cross-fades to the next one, a welcome panel that says what the portal does for each
 * role, and whatever the page projects (the form card) on the right.
 */
@Component({
  selector: 'app-entry-layout',
  imports: [AsyncPipe, DatePipe],
  templateUrl: './entry-layout.html',
  styleUrl: './entry-layout.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EntryLayout {
  /** The line under the greeting. */
  readonly subtitle = input('Sign in to pick up where you left off.');

  protected readonly scenes = PHOTO_SCENES;
  protected readonly roles = ROLE_INTROS;
  protected readonly rotateMs = ROTATE_EVERY_MS;
  protected readonly today = new Date();
  protected readonly period = periodOf(this.today.getHours());
  protected readonly greeting = greetingOf(this.today.getHours());
  protected readonly autoplay = !window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  private readonly slide = new BehaviorSubject(firstSlide());
  protected readonly slide$ = this.slide.asObservable();

  /** Photographs asked for by a click or by the timer; each is decoded before it is shown. */
  private readonly requests = new Subject<number>();

  constructor() {
    this.requests
      .pipe(
        map((index) => wrap(index, this.scenes.length)),
        filter((index) => index !== this.slide.value.current),
        // A newer request cancels one that is still decoding.
        switchMap((index) => from(preload(this.scenes[index].src)).pipe(map(() => index))),
        takeUntilDestroyed(),
      )
      .subscribe((index) => this.slide.next(advance(this.slide.value, index)));

    if (this.autoplay) {
      // The countdown restarts whenever the photograph changes, also after a click.
      this.slide
        .pipe(
          map((slide) => slide.current),
          distinctUntilChanged(),
          switchMap(() => timer(ROTATE_EVERY_MS)),
          takeUntilDestroyed(),
        )
        .subscribe(() => this.show(this.slide.value.current + 1));
    }
  }

  protected show(index: number): void {
    this.requests.next(index);
  }
}

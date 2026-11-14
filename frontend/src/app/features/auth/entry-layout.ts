import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BehaviorSubject, timer } from 'rxjs';
import { greetingOf, periodOf } from '../../core/time/day-period';
import { PHOTO_SCENES } from './photo-scenes';

const ROTATE_EVERY_MS = 12_000;

/**
 * The frame shared by the login and registration pages: a full-screen photograph that slowly
 * zooms and cross-fades to the next one, tinted for the visitor's time of day, with a greeting
 * on the left and whatever the page projects (the form card) on the right.
 */
@Component({
  selector: 'app-entry-layout',
  imports: [AsyncPipe],
  templateUrl: './entry-layout.html',
  styleUrl: './entry-layout.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EntryLayout {
  /** The line under the greeting. */
  readonly subtitle = input('Sign in to pick up where you left off.');

  protected readonly scenes = PHOTO_SCENES;
  private readonly hour = new Date().getHours();
  protected readonly period = periodOf(this.hour);
  protected readonly greeting = greetingOf(this.hour);

  private readonly index = new BehaviorSubject(0);
  protected readonly index$ = this.index.asObservable();

  /** Photographs already shown; they stay in the page so going back to one is instant. */
  private readonly shown = new Set<number>([0]);

  constructor() {
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (!reduceMotion) {
      timer(ROTATE_EVERY_MS, ROTATE_EVERY_MS)
        .pipe(takeUntilDestroyed())
        .subscribe(() => this.show(this.index.value + 1));
    }
  }

  protected show(index: number): void {
    const next = (index + this.scenes.length) % this.scenes.length;
    this.shown.add(next);
    this.index.next(next);
  }

  /** Only the current photograph, the next one, and those already seen are downloaded. */
  protected shouldLoad(index: number, current: number): boolean {
    return (
      index === current || index === (current + 1) % this.scenes.length || this.shown.has(index)
    );
  }
}

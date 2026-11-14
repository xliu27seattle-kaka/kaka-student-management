import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found-page',
  imports: [RouterLink],
  template: `
    <main class="missing">
      <p class="code">404</p>
      <h1>This page does not exist.</h1>
      <p>The address may be mistyped, or the page may have moved.</p>
      <a class="btn btn-primary" routerLink="/">Go to the start page</a>
    </main>
  `,
  styles: `
    .missing {
      min-height: 100dvh;
      display: grid;
      place-content: center;
      justify-items: start;
      gap: 12px;
      padding: 24px;
    }
    .code {
      font: 600 12px/16px var(--font-mono);
      color: var(--accent);
    }
    h1 {
      font: 800 clamp(28px, 5vw, 48px) / 1.1 var(--font);
      letter-spacing: -0.03em;
      color: var(--ink-1);
    }
    .btn {
      margin-top: 12px;
      text-decoration: none;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotFoundPage {}

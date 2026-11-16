import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProfessorProfile, CourseSection } from '../api/models';
import { meetingSummary } from './week-layout';

/** A professor's public profile: portrait, introduction, expertise, experience, awards, teaching. */
@Component({
  selector: 'app-professor-card',
  imports: [RouterLink],
  template: `
    @let p = profile();
    <div class="profile">
      <aside class="card who rise">
        @if (p.photoUrl) {
          <img
            class="portrait"
            [src]="p.photoUrl"
            [alt]="'Portrait of ' + p.firstName + ' ' + p.lastName"
          />
        } @else {
          <div class="portrait initials" aria-hidden="true">{{ initials() }}</div>
        }
        <h2>{{ p.firstName }} {{ p.lastName }}</h2>
        <p class="title">{{ p.title || 'Professor' }}</p>
        <span class="badge plain">{{ p.professorNo }}</span>
        @if (p.expertise.length) {
          <div class="chips">
            @for (item of p.expertise; track item) {
              <span class="tag">{{ item }}</span>
            }
          </div>
        }
      </aside>
      <div class="details">
        <section class="card rise" style="--i: 1">
          <h2>About</h2>
          <p class="bio">{{ p.bio || 'No introduction yet.' }}</p>
        </section>
        @if (sections()) {
          <section class="card rise" style="--i: 2">
            <h2>Teaching now</h2>
            @for (s of sections(); track s.id) {
              <div class="line">
                <div>
                  <b>{{ s.code }}</b> {{ s.courseTitle }}
                  <div class="hint">{{ s.semesterName }} · {{ summary(s) }}</div>
                </div>
                <span class="hint num">{{ s.enrolledCount }}/{{ s.capacity }}</span>
              </div>
            } @empty {
              <p class="hint">No sections in an open or running semester.</p>
            }
          </section>
        }
        <div class="grid-2">
          <section class="card rise" style="--i: 3">
            <h2>Experience</h2>
            @for (e of p.experience; track $index) {
              <div class="line">
                <div>
                  <b>{{ e.roleTitle }}</b>
                  <div class="hint">{{ e.organization }}</div>
                </div>
                <span class="hint">{{ e.period }}</span>
              </div>
            } @empty {
              <p class="hint">Nothing listed.</p>
            }
          </section>
          <section class="card rise" style="--i: 4">
            <h2>Awards</h2>
            @for (a of p.awards; track $index) {
              <div class="line">
                <b>{{ a.title }}</b>
                <span class="hint num">{{ a.year }}</span>
              </div>
            } @empty {
              <p class="hint">Nothing listed.</p>
            }
          </section>
        </div>
        @if (editLink()) {
          <a class="btn btn-ghost edit" [routerLink]="editLink()">
            <i class="ph ph-pencil-simple" aria-hidden="true"></i>Edit my profile
          </a>
        }
      </div>
    </div>
  `,
  styles: `
    .profile {
      display: grid;
      grid-template-columns: 300px minmax(0, 1fr);
      gap: 24px;
      align-items: start;
      @media (max-width: 960px) {
        grid-template-columns: minmax(0, 1fr);
      }
    }
    .who {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      text-align: center;
      h2 {
        margin: 8px 0 0;
        font: 700 20px/28px var(--font);
        color: var(--ink-1);
      }
    }
    .portrait {
      width: 160px;
      height: 160px;
      border-radius: 50%;
      object-fit: cover;
      background: var(--panel);
      box-shadow: 0 0 0 6px var(--accent-soft);
    }
    .initials {
      display: grid;
      place-items: center;
      font: 700 48px/1 var(--font);
      color: var(--accent);
    }
    .title {
      color: var(--ink-3);
    }
    .chips {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 6px;
      margin-top: 8px;
    }
    .tag {
      padding: 4px 10px;
      border-radius: 999px;
      background: var(--accent-soft);
      color: var(--accent);
      font: 600 12px/16px var(--font);
    }
    .details {
      display: flex;
      flex-direction: column;
      gap: 24px;
    }
    .bio {
      line-height: 24px;
      white-space: pre-line;
    }
    .line {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      padding: 10px 0;
      border-top: 1px solid var(--line);
      b {
        color: var(--ink-1);
      }
      &:first-of-type {
        border-top: 0;
        padding-top: 0;
      }
    }
    .edit {
      align-self: flex-start;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfessorCard {
  readonly profile = input.required<ProfessorProfile>();
  /** What the professor teaches now; leave out to hide the section. */
  readonly sections = input<CourseSection[] | null>(null);
  readonly editLink = input<string | null>(null);

  protected readonly initials = computed(
    () => this.profile().firstName.charAt(0) + this.profile().lastName.charAt(0),
  );

  protected summary(section: CourseSection): string {
    return meetingSummary(section.meetings);
  }
}

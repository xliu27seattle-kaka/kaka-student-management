import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Transcript } from '../api/models';
import { StatusBadge } from './status-badge';
import { Watermark } from './watermark';

/**
 * A transcript, laid out to read on screen and to print: semesters in order, each with its
 * courses and GPA, then the cumulative GPA and earned credits. Used by students and administrators.
 */
@Component({
  selector: 'app-transcript-sheet',
  imports: [DecimalPipe, DatePipe, StatusBadge, Watermark],
  template: `
    @let t = transcript();
    <article class="sheet print-area">
      <app-watermark />
      <div class="content">
        <header class="sheet-head">
          <div>
            <span class="eyebrow">Kaka Student Management · Academic transcript</span>
            <h2>{{ t.firstName }} {{ t.lastName }}</h2>
            <p class="muted">{{ t.studentNo }} · {{ t.email }}</p>
          </div>
          <div class="seal" [class.official]="t.official">
            {{ t.official ? 'Official' : 'Unofficial' }}
            @if (t.certificateNo) {
              <small>Certificate {{ t.certificateNo }}</small>
            }
          </div>
        </header>

        <dl class="facts summary">
          <div>
            <dt>Cumulative GPA</dt>
            <dd class="num big" data-testid="cumulative-gpa">
              {{ t.cumulativeGpa === null ? '—' : (t.cumulativeGpa | number: '1.2-2') }}
            </dd>
          </div>
          <div>
            <dt>Earned credits</dt>
            <dd class="num big">{{ t.earnedCredits }}</dd>
          </div>
          <div>
            <dt>Status</dt>
            <dd><app-status-badge [status]="t.academicStatus" /></dd>
          </div>
          @if (t.graduatedAt) {
            <div>
              <dt>Graduated</dt>
              <dd>{{ t.graduatedAt | date: 'longDate' }}</dd>
            </div>
          }
        </dl>

        @for (term of t.terms; track term.semesterId) {
          <section class="term">
            <div class="term-head">
              <h3>{{ term.semesterName }}</h3>
              <span class="muted">
                @if (term.semesterStatus !== 'ENDED') {
                  In progress
                } @else {
                  Term GPA
                  <b class="num">{{ term.gpa === null ? '—' : (term.gpa | number: '1.2-2') }}</b>
                }
              </span>
            </div>
            <table class="table">
              <thead>
                <tr>
                  <th>Course</th>
                  <th>Section</th>
                  <th>Professor</th>
                  <th class="right">Credits</th>
                  <th class="right">Score</th>
                  <th class="right">Grade</th>
                  <th class="right">Points</th>
                </tr>
              </thead>
              <tbody>
                @for (row of term.rows; track row.sectionCode) {
                  <tr>
                    <td>
                      <span class="strong">{{ row.courseCode }}</span>
                      <span class="muted"> {{ row.courseTitle }}</span>
                    </td>
                    <td>{{ row.sectionCode }}</td>
                    <td>{{ row.professorName }}</td>
                    <td class="right num">{{ row.credits }}</td>
                    <td class="right num">{{ row.score ?? '—' }}</td>
                    <td class="right strong">
                      {{ row.letter ?? (row.status === 'IN_PROGRESS' ? 'IP' : '—') }}
                    </td>
                    <td class="right num">
                      {{ row.points === null ? '—' : (row.points | number: '1.1-1') }}
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </section>
        } @empty {
          <p class="muted">No courses yet.</p>
        }
        <footer class="sheet-foot">
          <span class="studio-mark">Kaka Studio</span>
          <span class="muted">W = withdrawn, not counted in GPA · IP = in progress</span>
        </footer>
      </div>
    </article>
  `,
  styleUrl: './printable-sheet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TranscriptSheet {
  readonly transcript = input.required<Transcript>();
}

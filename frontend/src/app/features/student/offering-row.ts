import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Offering } from '../../shared/api/models';
import { meetingSummary } from '../../shared/ui/week-layout';

/**
 * One section on the enrollment page. It only displays and reports: the parent decides what an
 * enroll, drop or preview means (inputs in, outputs out).
 */
@Component({
  selector: 'app-offering-row',
  imports: [RouterLink],
  template: `
    @let o = offering();
    <article
      class="row"
      [class.mine]="o.myStatus === 'ENROLLED'"
      [class.blocked]="!o.available && o.myStatus !== 'ENROLLED'"
      (mouseenter)="previewed.emit(o.section.id)"
      (mouseleave)="previewed.emit(null)"
      (focusin)="previewed.emit(o.section.id)"
      [attr.data-testid]="'offering-' + o.section.code"
    >
      <div class="main">
        <div class="title">
          <b>{{ o.section.code }}</b>
          <span>{{ o.section.courseTitle }}</span>
        </div>
        <div class="meta hint">
          <span><i class="ph ph-clock" aria-hidden="true"></i>{{ summary() }}</span>
          <a [routerLink]="['/student/professors', o.section.professorId]"
            ><i class="ph ph-chalkboard-teacher" aria-hidden="true"></i
            >{{ o.section.professorName }}</a
          >
          <span>{{ o.section.credits }} credits</span>
          @if (o.section.room) {
            <span>{{ o.section.room }}</span>
          }
        </div>
        @if (!o.available && o.myStatus !== 'ENROLLED') {
          <div class="reason" [title]="otherReasons()">
            <i class="ph ph-lock-simple" aria-hidden="true"></i>{{ o.reasons[0]?.message }}
            @if (o.reasons.length > 1) {
              <span class="more">+{{ o.reasons.length - 1 }} more</span>
            }
          </div>
        }
      </div>
      <div class="side">
        <span class="seats num" [class.full]="o.section.enrolledCount >= o.section.capacity">
          {{ o.section.enrolledCount }}/{{ o.section.capacity }}
        </span>
        @if (o.myStatus === 'ENROLLED') {
          <button
            type="button"
            class="btn btn-sm btn-ghost"
            [disabled]="busy() || !canDrop()"
            (click)="dropped.emit(o)"
          >
            {{ o.section.semesterStatus === 'STARTED' ? 'Withdraw' : 'Drop' }}
          </button>
        } @else {
          <button
            type="button"
            class="btn btn-sm btn-primary"
            [disabled]="!o.available || busy()"
            (click)="enrolled.emit(o)"
          >
            {{ busy() ? 'Working…' : 'Enroll' }}
          </button>
        }
      </div>
    </article>
  `,
  styleUrl: './offering-row.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OfferingRow {
  readonly offering = input.required<Offering>();
  readonly busy = input(false);
  readonly enrolled = output<Offering>();
  readonly dropped = output<Offering>();
  readonly previewed = output<number | null>();

  protected readonly summary = computed(() => meetingSummary(this.offering().section.meetings));
  protected readonly otherReasons = computed(() =>
    this.offering()
      .reasons.map((reason) => reason.message)
      .join(' · '),
  );
  protected readonly canDrop = computed(() =>
    ['READY', 'STARTED'].includes(this.offering().section.semesterStatus),
  );
}

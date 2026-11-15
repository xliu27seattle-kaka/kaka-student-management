import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DAY_SHORT, ScheduleEntry, layoutWeek } from './week-layout';

/** A weekly timetable. The enrollment page feeds it the student's sections plus a preview. */
@Component({
  selector: 'app-schedule-grid',
  template: `
    @let week = layout();
    <div class="week" [style.--days]="week.days.length" [style.--hours]="week.hours.length">
      <div class="corner"></div>
      @for (day of week.days; track day) {
        <div class="day-head">{{ dayShort[day] }}</div>
      }
      <div class="hours">
        @for (hour of week.hours; track hour) {
          <span>{{ hour }}:00</span>
        }
      </div>
      @for (day of week.days; track day) {
        <div class="day" [attr.aria-label]="dayShort[day]">
          @for (block of week.blocks; track block.key) {
            @if (block.day === day) {
              <div
                class="block"
                [class.preview]="block.tone === 'preview'"
                [style.top.%]="block.top"
                [style.height.%]="block.height"
                [title]="block.code + ' · ' + block.title + ' · ' + block.time"
              >
                <b>{{ block.code }}</b>
                <span>{{ block.time }}</span>
              </div>
            }
          }
        </div>
      }
    </div>
    @if (entries().length === 0) {
      <p class="hint empty">No classes yet. Sections you enroll in appear here.</p>
    }
  `,
  styles: `
    :host {
      display: block;
    }
    .week {
      display: grid;
      grid-template-columns: 44px repeat(var(--days), minmax(0, 1fr));
      grid-template-rows: 28px calc(var(--hours) * 34px);
      gap: 0 4px;
    }
    .day-head {
      font: 600 12px/28px var(--font);
      color: var(--ink-3);
      text-align: center;
      text-transform: uppercase;
      letter-spacing: 0.06em;
    }
    .hours {
      display: grid;
      grid-template-rows: repeat(var(--hours), 1fr);
      font: 500 11px/1 var(--font-num);
      color: var(--ink-3);
      span {
        transform: translateY(-5px);
      }
    }
    .day {
      position: relative;
      border-radius: 8px;
      background: repeating-linear-gradient(
        to bottom,
        var(--panel) 0,
        var(--panel) 33px,
        var(--line) 33px,
        var(--line) 34px
      );
    }
    .block {
      position: absolute;
      left: 3px;
      right: 3px;
      display: flex;
      flex-direction: column;
      gap: 1px;
      padding: 4px 6px;
      border-radius: 6px;
      overflow: hidden;
      background: var(--accent);
      color: var(--on-accent);
      font-size: 11px;
      line-height: 14px;
      animation: pop 0.35s var(--ease) both;
      b {
        font-size: 12px;
      }
      &.preview {
        background: var(--accent-soft);
        color: var(--accent);
        outline: 1.5px dashed var(--accent);
        outline-offset: -1.5px;
      }
    }
    .empty {
      margin-top: 12px;
      text-align: center;
    }
    @keyframes pop {
      from {
        opacity: 0;
        transform: scale(0.94);
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ScheduleGrid {
  readonly entries = input.required<ScheduleEntry[]>();
  protected readonly layout = computed(() => layoutWeek(this.entries()));
  protected readonly dayShort = DAY_SHORT;
}

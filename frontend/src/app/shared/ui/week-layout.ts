import { DayOfWeek, Meeting } from '../api/models';

export interface ScheduleEntry {
  key: string;
  code: string;
  title: string;
  meetings: Meeting[];
  /** "enrolled" blocks are solid; a "preview" block shows a section the student is looking at. */
  tone: 'enrolled' | 'preview';
}

export interface PlacedBlock {
  key: string;
  code: string;
  title: string;
  tone: ScheduleEntry['tone'];
  day: DayOfWeek;
  time: string;
  /** Position inside the day column, in percent of its height. */
  top: number;
  height: number;
}

export interface WeekLayout {
  days: DayOfWeek[];
  hours: number[];
  blocks: PlacedBlock[];
}

const WEEKDAYS: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'];
const WEEKEND: DayOfWeek[] = ['SATURDAY', 'SUNDAY'];

export function minutes(time: string): number {
  const [h, m] = time.split(':').map(Number);
  return h * 60 + m;
}

/**
 * Places every meeting on a week grid. The grid runs from the earliest start to the latest end
 * (at least 8:00 to 18:00), Monday to Friday, plus the weekend only if something meets then.
 */
export function layoutWeek(entries: ScheduleEntry[]): WeekLayout {
  const meetings = entries.flatMap((entry) => entry.meetings);
  const first = Math.min(8 * 60, ...meetings.map((m) => minutes(m.start)));
  const last = Math.max(18 * 60, ...meetings.map((m) => minutes(m.end)));
  const startHour = Math.floor(first / 60);
  const endHour = Math.ceil(last / 60);
  const span = (endHour - startHour) * 60;
  const days = [...WEEKDAYS, ...WEEKEND.filter((day) => meetings.some((m) => m.day === day))];

  const blocks = entries.flatMap((entry) =>
    entry.meetings.map((meeting) => ({
      key: entry.key + '-' + meeting.day + '-' + meeting.start,
      code: entry.code,
      title: entry.title,
      tone: entry.tone,
      day: meeting.day,
      time: short(meeting.start) + '–' + short(meeting.end),
      top: ((minutes(meeting.start) - startHour * 60) / span) * 100,
      height: ((minutes(meeting.end) - minutes(meeting.start)) / span) * 100,
    })),
  );
  const hours = Array.from({ length: endHour - startHour }, (_, i) => startHour + i);
  return { days, hours, blocks };
}

/** "10:00:00" or "10:00" to "10:00". */
export function short(time: string): string {
  return time.slice(0, 5);
}

export const DAY_SHORT: Record<DayOfWeek, string> = {
  MONDAY: 'Mon',
  TUESDAY: 'Tue',
  WEDNESDAY: 'Wed',
  THURSDAY: 'Thu',
  FRIDAY: 'Fri',
  SATURDAY: 'Sat',
  SUNDAY: 'Sun',
};

/** "Mon/Wed 10:00–11:15" style summary of a section's meetings. */
export function meetingSummary(meetings: Meeting[]): string {
  const byTime = new Map<string, string[]>();
  for (const m of meetings) {
    const time = short(m.start) + '–' + short(m.end);
    byTime.set(time, [...(byTime.get(time) ?? []), DAY_SHORT[m.day]]);
  }
  return [...byTime.entries()].map(([time, days]) => days.join('/') + ' ' + time).join(', ');
}

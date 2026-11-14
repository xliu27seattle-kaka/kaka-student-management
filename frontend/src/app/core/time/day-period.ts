/** The part of the day the entry page tints its photograph for. */
export type DayPeriod = 'dawn' | 'day' | 'dusk' | 'night';

/** @param hour local hour of the day, 0 to 23 */
export function periodOf(hour: number): DayPeriod {
  if (hour >= 5 && hour < 8) return 'dawn';
  if (hour >= 8 && hour < 17) return 'day';
  if (hour >= 17 && hour < 20) return 'dusk';
  return 'night';
}

/** @param hour local hour of the day, 0 to 23 */
export function greetingOf(hour: number): string {
  if (hour >= 5 && hour < 12) return 'Good morning.';
  if (hour >= 12 && hour < 18) return 'Good afternoon.';
  return 'Good evening.';
}

/**
 * The letter for a score, for the live preview while a professor types. The server decides the
 * real grade with the same scale (StandardGradeScale); this copy only helps the eye.
 */
const BANDS: [number, string][] = [
  [93, 'A'],
  [90, 'A-'],
  [87, 'B+'],
  [83, 'B'],
  [80, 'B-'],
  [77, 'C+'],
  [73, 'C'],
  [70, 'C-'],
  [60, 'D'],
  [0, 'F'],
];

export function letterFor(score: number): string {
  return BANDS.find(([minimum]) => score >= minimum)?.[1] ?? '—';
}

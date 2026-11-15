import { layoutWeek, meetingSummary } from './week-layout';
import { letterFor } from './grade-scale';

describe('layoutWeek', () => {
  it('places a meeting by its time inside an 8:00 to 18:00 day', () => {
    const week = layoutWeek([
      {
        key: '1',
        code: 'CS201-A',
        title: 'Data Structures',
        tone: 'enrolled',
        meetings: [{ day: 'MONDAY', start: '13:00:00', end: '14:15:00' }],
      },
    ]);

    expect(week.days).toEqual(['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']);
    expect(week.hours[0]).toBe(8);
    expect(week.hours.at(-1)).toBe(17);
    expect(week.blocks[0].top).toBeCloseTo(50); // 13:00 is half way through 8 to 18
    expect(week.blocks[0].height).toBeCloseTo(12.5); // 75 of 600 minutes
    expect(week.blocks[0].time).toBe('13:00–14:15');
  });

  it('grows for early classes and adds the weekend only when needed', () => {
    const week = layoutWeek([
      {
        key: '1',
        code: 'X',
        title: 'X',
        tone: 'preview',
        meetings: [{ day: 'SATURDAY', start: '07:30', end: '09:00' }],
      },
    ]);
    expect(week.hours[0]).toBe(7);
    expect(week.days).toContain('SATURDAY');
    expect(week.days).not.toContain('SUNDAY');
  });

  it('summarises meetings with the same time together', () => {
    expect(
      meetingSummary([
        { day: 'MONDAY', start: '10:00:00', end: '11:15:00' },
        { day: 'WEDNESDAY', start: '10:00:00', end: '11:15:00' },
        { day: 'FRIDAY', start: '09:00:00', end: '09:50:00' },
      ]),
    ).toBe('Mon/Wed 10:00–11:15, Fri 09:00–09:50');
  });
});

describe('letterFor', () => {
  it('follows the same boundaries as the server', () => {
    expect(letterFor(93)).toBe('A');
    expect(letterFor(92)).toBe('A-');
    expect(letterFor(60)).toBe('D');
    expect(letterFor(59)).toBe('F');
  });
});

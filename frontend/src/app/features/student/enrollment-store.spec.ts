import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ToastStore } from '../../core/ui/toast-store';
import { Enrollment, Offering, Semester } from '../../shared/api/models';
import { StudentApi } from '../../shared/api/student-api';
import { EnrollmentStore } from './enrollment-store';

const semester: Semester = {
  id: 3,
  code: '2027SP',
  name: 'Spring 2027',
  startDate: '2027-01-11',
  endDate: '2027-05-07',
  status: 'READY',
  allowedNext: ['STARTED', 'DRAFT'],
  sectionCount: 2,
  enrolledCount: 0,
};

function offering(id: number, mine: boolean, available = true): Offering {
  return {
    section: {
      id,
      code: 'CS10' + id + '-A',
      letter: 'A',
      courseId: id,
      courseCode: 'CS10' + id,
      courseTitle: 'Course ' + id,
      credits: 3,
      semesterId: 3,
      semesterCode: '2027SP',
      semesterName: 'Spring 2027',
      semesterStatus: 'READY',
      professorId: 1,
      professorName: 'Amara Okafor',
      capacity: 20,
      enrolledCount: 5,
      room: null,
      meetings: [{ day: 'MONDAY', start: '10:00', end: '11:00' }],
    },
    available,
    reasons: available ? [] : [{ code: 'SECTION_FULL', message: 'Section full' }],
    myEnrollmentId: mine ? 100 + id : null,
    myStatus: mine ? 'ENROLLED' : null,
  };
}

function setup(api: Partial<StudentApi>) {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    providers: [EnrollmentStore, { provide: StudentApi, useValue: api }],
  });
  return TestBed.inject(EnrollmentStore);
}

describe('EnrollmentStore', () => {
  it('loads the sections of the chosen semester', async () => {
    const store = setup({ offerings: () => of([offering(1, true), offering(2, false)]) });
    store.select(semester);

    const state = await firstValueFrom(store.state$);
    expect(state.offerings.length).toBe(2);
    expect(await firstValueFrom(store.credits$)).toBe(3);
  });

  it('draws my sections and the previewed one on the timetable', async () => {
    const store = setup({ offerings: () => of([offering(1, true), offering(2, false)]) });
    store.select(semester);
    store.preview(2);

    const schedule = await firstValueFrom(store.schedule$);
    expect(schedule.map((e) => [e.code, e.tone])).toEqual([
      ['CS101-A', 'enrolled'],
      ['CS102-A', 'preview'],
    ]);
  });

  it('enrolling reloads the list, so every part of the page agrees', async () => {
    let enrolled = false;
    const store = setup({
      offerings: () => of([offering(1, enrolled)]),
      enroll: () => {
        enrolled = true;
        return of({} as Enrollment);
      },
    });
    store.select(semester);
    store.enroll(offering(1, false));

    expect(await firstValueFrom(store.credits$)).toBe(3);
    expect((await firstValueFrom(store.state$)).busyId).toBeNull();
  });

  it('shows the server reason when enrolling is refused', () => {
    const store = setup({
      offerings: () => of([offering(1, false)]),
      enroll: () =>
        throwError(
          () =>
            new HttpErrorResponse({
              status: 422,
              error: { code: 'NOT_ELIGIBLE', message: 'Section full', fieldErrors: [] },
            }),
        ),
    });
    const toasts = TestBed.inject(ToastStore);
    const shown = vi.spyOn(toasts, 'error');
    store.select(semester);
    store.enroll(offering(1, false));

    expect(shown).toHaveBeenCalledWith('Section full');
  });
});

describe('EnrollmentStore when semesters change quickly', () => {
  it('shows the sections of the semester chosen last, even if an older answer arrives later', async () => {
    const { Subject } = await import('rxjs');
    const answers = new Map<number, InstanceType<typeof Subject<Offering[]>>>();
    const store = setup({
      offerings: (id: number) => {
        const answer = new Subject<Offering[]>();
        answers.set(id, answer);
        return answer;
      },
    });
    store.select({ ...semester, id: 1, name: 'First' });
    store.select({ ...semester, id: 2, name: 'Second' });

    answers.get(2)!.next([offering(2, false)]);
    answers.get(1)!.next([offering(1, false), offering(3, false)]); // late answer for the old one

    const state = await firstValueFrom(store.state$);
    expect(state.semester?.name).toBe('Second');
    expect(state.offerings.map((o) => o.section.id)).toEqual([2]);
  });
});

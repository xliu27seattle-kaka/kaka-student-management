import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ApplicationStatus,
  CourseDetail,
  CourseRequest,
  Dashboard,
  Enrollment,
  GraduationApplication,
  Offering,
  Page,
  PersonRequest,
  Professor,
  ProfileRequest,
  Section,
  SectionDetail,
  SectionRequest,
  Semester,
  SemesterRequest,
  SemesterStatus,
  SentNotification,
  Student,
  Transcript,
  UserStatus,
  Audience,
} from './models';

const ADMIN = '/api/v1/admin';

/** Everything an administrator can do. Each method is one backend endpoint. */
@Injectable({ providedIn: 'root' })
export class AdminApi {
  private readonly http = inject(HttpClient);

  dashboard(): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${ADMIN}/dashboard`);
  }

  // ── students ──
  students(filter: {
    status?: UserStatus | '';
    q?: string;
    page?: number;
  }): Observable<Page<Student>> {
    return this.http.get<Page<Student>>(`${ADMIN}/students`, { params: params(filter) });
  }

  student(id: number): Observable<Student> {
    return this.http.get<Student>(`${ADMIN}/students/${id}`);
  }

  createStudent(request: PersonRequest): Observable<Student> {
    return this.http.post<Student>(`${ADMIN}/students`, request);
  }

  updateStudent(id: number, request: PersonRequest): Observable<Student> {
    return this.http.put<Student>(`${ADMIN}/students/${id}`, request);
  }

  studentAction(
    id: number,
    action: 'approve' | 'reject' | 'deactivate' | 'reactivate',
  ): Observable<Student> {
    return this.http.post<Student>(`${ADMIN}/students/${id}/${action}`, {});
  }

  deleteStudent(id: number): Observable<void> {
    return this.http.delete<void>(`${ADMIN}/students/${id}`);
  }

  studentEnrollments(id: number): Observable<Enrollment[]> {
    return this.http.get<Enrollment[]>(`${ADMIN}/students/${id}/enrollments`);
  }

  studentOfferings(id: number, semesterId: number): Observable<Offering[]> {
    return this.http.get<Offering[]>(`${ADMIN}/students/${id}/offerings`, {
      params: { semesterId },
    });
  }

  enrollStudent(id: number, sectionId: number): Observable<Enrollment> {
    return this.http.post<Enrollment>(`${ADMIN}/students/${id}/enrollments`, { sectionId });
  }

  dropStudent(id: number, enrollmentId: number): Observable<Enrollment> {
    return this.http.delete<Enrollment>(`${ADMIN}/students/${id}/enrollments/${enrollmentId}`);
  }

  studentTranscript(id: number): Observable<Transcript> {
    return this.http.get<Transcript>(`${ADMIN}/students/${id}/transcript`);
  }

  // ── professors ──
  professors(filter: { q?: string; page?: number; size?: number }): Observable<Page<Professor>> {
    return this.http.get<Page<Professor>>(`${ADMIN}/professors`, { params: params(filter) });
  }

  professor(id: number): Observable<Professor> {
    return this.http.get<Professor>(`${ADMIN}/professors/${id}`);
  }

  createProfessor(
    request: PersonRequest & { profile: ProfileRequest | null },
  ): Observable<Professor> {
    return this.http.post<Professor>(`${ADMIN}/professors`, request);
  }

  updateProfessor(id: number, request: PersonRequest): Observable<Professor> {
    return this.http.put<Professor>(`${ADMIN}/professors/${id}`, request);
  }

  professorAction(id: number, action: 'deactivate' | 'reactivate'): Observable<Professor> {
    return this.http.post<Professor>(`${ADMIN}/professors/${id}/${action}`, {});
  }

  deleteProfessor(id: number): Observable<void> {
    return this.http.delete<void>(`${ADMIN}/professors/${id}`);
  }

  // ── courses ──
  createCourse(request: CourseRequest): Observable<CourseDetail> {
    return this.http.post<CourseDetail>(`${ADMIN}/courses`, request);
  }

  updateCourse(id: number, request: CourseRequest): Observable<CourseDetail> {
    return this.http.put<CourseDetail>(`${ADMIN}/courses/${id}`, request);
  }

  deleteCourse(id: number): Observable<void> {
    return this.http.delete<void>(`${ADMIN}/courses/${id}`);
  }

  // ── semesters and sections ──
  semesters(): Observable<Semester[]> {
    return this.http.get<Semester[]>(`${ADMIN}/semesters`);
  }

  semester(id: number): Observable<Semester> {
    return this.http.get<Semester>(`${ADMIN}/semesters/${id}`);
  }

  createSemester(request: SemesterRequest): Observable<Semester> {
    return this.http.post<Semester>(`${ADMIN}/semesters`, request);
  }

  updateSemester(id: number, request: SemesterRequest): Observable<Semester> {
    return this.http.put<Semester>(`${ADMIN}/semesters/${id}`, request);
  }

  deleteSemester(id: number): Observable<void> {
    return this.http.delete<void>(`${ADMIN}/semesters/${id}`);
  }

  ungradedSections(id: number): Observable<string[]> {
    return this.http.get<string[]>(`${ADMIN}/semesters/${id}/ungraded-sections`);
  }

  transition(id: number, target: SemesterStatus): Observable<Semester> {
    return this.http.post<Semester>(`${ADMIN}/semesters/${id}/transition`, { target });
  }

  sections(semesterId: number): Observable<Section[]> {
    return this.http.get<Section[]>(`${ADMIN}/semesters/${semesterId}/sections`);
  }

  courseSections(courseId: number): Observable<Section[]> {
    return this.http.get<Section[]>(`${ADMIN}/courses/${courseId}/sections`);
  }

  section(id: number): Observable<SectionDetail> {
    return this.http.get<SectionDetail>(`${ADMIN}/sections/${id}`);
  }

  createSection(semesterId: number, request: SectionRequest): Observable<Section> {
    return this.http.post<Section>(`${ADMIN}/semesters/${semesterId}/sections`, request);
  }

  updateSection(id: number, request: SectionRequest): Observable<Section> {
    return this.http.put<Section>(`${ADMIN}/sections/${id}`, request);
  }

  deleteSection(id: number): Observable<void> {
    return this.http.delete<void>(`${ADMIN}/sections/${id}`);
  }

  // ── graduation and notifications ──
  applications(status: ApplicationStatus | ''): Observable<GraduationApplication[]> {
    return this.http.get<GraduationApplication[]>(`${ADMIN}/graduation-applications`, {
      params: params({ status }),
    });
  }

  decide(id: number, decision: 'approve' | 'reject'): Observable<GraduationApplication> {
    return this.http.post<GraduationApplication>(
      `${ADMIN}/graduation-applications/${id}/${decision}`,
      {},
    );
  }

  sendNotification(request: {
    audience: Audience;
    subject: string;
    body: string;
  }): Observable<SentNotification> {
    return this.http.post<SentNotification>(`${ADMIN}/notifications`, request);
  }

  sentNotifications(): Observable<SentNotification[]> {
    return this.http.get<SentNotification[]>(`${ADMIN}/notifications`);
  }
}

/** Query parameters from an object, leaving out empty values. */
export function params(
  values: Record<string, string | number | boolean | null | undefined>,
): HttpParams {
  let result = new HttpParams();
  for (const [key, value] of Object.entries(values)) {
    if (value !== null && value !== undefined && value !== '') {
      result = result.set(key, String(value));
    }
  }
  return result;
}

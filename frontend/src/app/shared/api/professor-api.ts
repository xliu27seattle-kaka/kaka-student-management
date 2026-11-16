import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { params } from './admin-api';
import {
  ProfessorProfile,
  ProfileRequest,
  CourseSection,
  CourseSectionDetail,
  SentNotification,
} from './models';

const PROFESSOR = '/api/v1/professor';

/** The signed-in professor's own teaching and profile. */
@Injectable({ providedIn: 'root' })
export class ProfessorApi {
  private readonly http = inject(HttpClient);

  sections(semesterId?: number): Observable<CourseSection[]> {
    return this.http.get<CourseSection[]>(`${PROFESSOR}/sections`, {
      params: params({ semesterId }),
    });
  }

  section(id: number): Observable<CourseSectionDetail> {
    return this.http.get<CourseSectionDetail>(`${PROFESSOR}/sections/${id}`);
  }

  saveGrades(
    id: number,
    grades: { enrollmentId: number; score: number | null }[],
  ): Observable<CourseSectionDetail> {
    return this.http.put<CourseSectionDetail>(`${PROFESSOR}/sections/${id}/grades`, { grades });
  }

  notifySection(id: number, subject: string, body: string): Observable<SentNotification> {
    return this.http.post<SentNotification>(`${PROFESSOR}/sections/${id}/notifications`, {
      subject,
      body,
    });
  }

  sentNotifications(): Observable<SentNotification[]> {
    return this.http.get<SentNotification[]>(`${PROFESSOR}/notifications`);
  }

  myProfile(): Observable<ProfessorProfile> {
    return this.http.get<ProfessorProfile>(`${PROFESSOR}/profile`);
  }

  updateMyProfile(request: ProfileRequest): Observable<ProfessorProfile> {
    return this.http.put<ProfessorProfile>(`${PROFESSOR}/profile`, request);
  }
}

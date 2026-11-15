import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { params } from './admin-api';
import {
  CourseDetail,
  CourseSummary,
  InboxItem,
  Page,
  ProfessorProfile,
  Section,
  Semester,
} from './models';

/** Endpoints every signed-in user may call, whatever their role. */
@Injectable({ providedIn: 'root' })
export class CommonApi {
  private readonly http = inject(HttpClient);

  courses(filter: {
    q?: string;
    department?: string;
    active?: boolean | '';
    page?: number;
    size?: number;
  }): Observable<Page<CourseSummary>> {
    return this.http.get<Page<CourseSummary>>('/api/v1/courses', { params: params(filter) });
  }

  course(id: number): Observable<CourseDetail> {
    return this.http.get<CourseDetail>(`/api/v1/courses/${id}`);
  }

  /** Semesters students and professors can see (no drafts). */
  semesters(): Observable<Semester[]> {
    return this.http.get<Semester[]>('/api/v1/semesters');
  }

  professorDirectory(): Observable<ProfessorProfile[]> {
    return this.http.get<ProfessorProfile[]>('/api/v1/professors');
  }

  professorProfile(id: number): Observable<ProfessorProfile> {
    return this.http.get<ProfessorProfile>(`/api/v1/professors/${id}`);
  }

  professorSections(id: number): Observable<Section[]> {
    return this.http.get<Section[]>(`/api/v1/professors/${id}/sections`);
  }

  unreadCount(): Observable<{ unread: number }> {
    return this.http.get<{ unread: number }>('/api/v1/notifications/unread-count');
  }

  inbox(page = 0): Observable<Page<InboxItem>> {
    return this.http.get<Page<InboxItem>>('/api/v1/notifications', { params: { page, size: 50 } });
  }

  markRead(id: number): Observable<void> {
    return this.http.post<void>(`/api/v1/notifications/${id}/read`, {});
  }

  markAllRead(): Observable<void> {
    return this.http.post<void>('/api/v1/notifications/read-all', {});
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>('/api/v1/auth/password', { currentPassword, newPassword });
  }
}

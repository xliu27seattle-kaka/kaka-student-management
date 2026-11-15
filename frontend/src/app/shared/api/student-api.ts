import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { params } from './admin-api';
import { Enrollment, GraduationStatus, Offering, Transcript } from './models';

const STUDENT = '/api/v1/student';

/** The signed-in student's own records. */
@Injectable({ providedIn: 'root' })
export class StudentApi {
  private readonly http = inject(HttpClient);

  offerings(semesterId: number): Observable<Offering[]> {
    return this.http.get<Offering[]>(`${STUDENT}/semesters/${semesterId}/offerings`);
  }

  enrollments(semesterId?: number): Observable<Enrollment[]> {
    return this.http.get<Enrollment[]>(`${STUDENT}/enrollments`, {
      params: params({ semesterId }),
    });
  }

  enroll(sectionId: number): Observable<Enrollment> {
    return this.http.post<Enrollment>(`${STUDENT}/enrollments`, { sectionId });
  }

  drop(enrollmentId: number): Observable<Enrollment> {
    return this.http.delete<Enrollment>(`${STUDENT}/enrollments/${enrollmentId}`);
  }

  transcript(): Observable<Transcript> {
    return this.http.get<Transcript>(`${STUDENT}/transcript`);
  }

  graduation(): Observable<GraduationStatus> {
    return this.http.get<GraduationStatus>(`${STUDENT}/graduation`);
  }

  applyForGraduation(): Observable<GraduationStatus> {
    return this.http.post<GraduationStatus>(`${STUDENT}/graduation/apply`, {});
  }
}

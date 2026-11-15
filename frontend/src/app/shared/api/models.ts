/** The shapes the backend sends and receives. They mirror the Java DTO records. */

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export type UserStatus = 'PENDING' | 'ACTIVE' | 'REJECTED' | 'INACTIVE';
export type AcademicStatus = 'ENROLLED' | 'GRADUATED';
export type SemesterStatus = 'DRAFT' | 'READY' | 'STARTED' | 'ENDED';
export type EnrollmentStatus = 'ENROLLED' | 'DROPPED' | 'WITHDRAWN' | 'COMPLETED';
export type DayOfWeek =
  'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

// ── people ──
export interface Student {
  id: number;
  studentNo: string;
  email: string;
  firstName: string;
  lastName: string;
  status: UserStatus;
  academicStatus: AcademicStatus;
  createdAt: string;
}

export interface PersonRequest {
  email: string;
  firstName: string;
  lastName: string;
}

export interface Professor {
  id: number;
  professorNo: string;
  email: string;
  firstName: string;
  lastName: string;
  status: UserStatus;
  title: string | null;
}

export interface Experience {
  roleTitle: string;
  organization: string;
  period: string;
}

export interface Award {
  title: string;
  year: number;
}

export interface ProfessorProfile {
  id: number;
  professorNo: string;
  firstName: string;
  lastName: string;
  title: string | null;
  bio: string | null;
  photoUrl: string | null;
  expertise: string[];
  experience: Experience[];
  awards: Award[];
}

export interface ProfileRequest {
  title: string;
  bio: string;
  expertise: string[];
  experience: Experience[];
  awards: Award[];
}

// ── catalogue ──
export interface CourseRef {
  id: number;
  code: string;
  title: string;
}

export interface CourseSummary {
  id: number;
  code: string;
  title: string;
  department: string;
  credits: number;
  active: boolean;
  prerequisites: CourseRef[];
}

export interface CourseDetail extends CourseSummary {
  description: string | null;
  requiredBy: CourseRef[];
  sectionCount: number;
}

export interface CourseRequest {
  code: string;
  title: string;
  description: string;
  department: string;
  credits: number;
  active: boolean;
  prerequisiteIds: number[];
}

export interface Semester {
  id: number;
  code: string;
  name: string;
  startDate: string;
  endDate: string;
  status: SemesterStatus;
  allowedNext: SemesterStatus[];
  sectionCount: number;
  enrolledCount: number;
}

export interface SemesterRequest {
  code: string;
  name: string;
  startDate: string;
  endDate: string;
}

export interface Meeting {
  day: DayOfWeek;
  start: string;
  end: string;
  label?: string;
}

export interface Section {
  id: number;
  code: string;
  letter: string;
  courseId: number;
  courseCode: string;
  courseTitle: string;
  credits: number;
  semesterId: number;
  semesterCode: string;
  semesterName: string;
  semesterStatus: SemesterStatus;
  professorId: number;
  professorName: string;
  capacity: number;
  enrolledCount: number;
  room: string | null;
  meetings: Meeting[];
}

export interface RosterEntry {
  enrollmentId: number;
  studentId: number;
  studentNo: string;
  firstName: string;
  lastName: string;
  email: string;
  status: EnrollmentStatus;
  score: number | null;
  letterGrade: string | null;
}

export interface SectionDetail {
  section: Section;
  roster: RosterEntry[];
}

export interface SectionRequest {
  courseId: number;
  professorId: number;
  capacity: number | null;
  room: string;
  meetings: Meeting[];
}

// ── enrollment ──
export interface Reason {
  code: string;
  message: string;
}

export interface Offering {
  section: Section;
  available: boolean;
  reasons: Reason[];
  myEnrollmentId: number | null;
  myStatus: EnrollmentStatus | null;
}

export interface Enrollment {
  id: number;
  status: EnrollmentStatus;
  section: Section;
  score: number | null;
  letterGrade: string | null;
}

// ── transcript and graduation ──
export type RowStatus = 'GRADED' | 'WITHDRAWN' | 'IN_PROGRESS';

export interface TranscriptRow {
  courseCode: string;
  courseTitle: string;
  sectionCode: string;
  professorName: string;
  credits: number;
  status: RowStatus;
  score: number | null;
  letter: string | null;
  points: number | null;
}

export interface TermRecord {
  semesterId: number;
  semesterCode: string;
  semesterName: string;
  semesterStatus: SemesterStatus;
  gpa: number | null;
  gradedCredits: number;
  rows: TranscriptRow[];
}

export interface Transcript {
  studentId: number;
  studentNo: string;
  firstName: string;
  lastName: string;
  email: string;
  academicStatus: AcademicStatus;
  certificateNo: string | null;
  graduatedAt: string | null;
  official: boolean;
  cumulativeGpa: number | null;
  earnedCredits: number;
  terms: TermRecord[];
}

export type ApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface GraduationApplication {
  id: number;
  studentId: number;
  studentNo: string;
  studentName: string;
  email: string;
  status: ApplicationStatus;
  appliedAt: string;
  decidedAt: string | null;
  earnedCredits: number;
}

export interface GraduationStatus {
  studentId: number;
  studentNo: string;
  firstName: string;
  lastName: string;
  academicStatus: AcademicStatus;
  earnedCredits: number;
  requiredCredits: number;
  eligible: boolean;
  reasons: string[];
  latestApplication: GraduationApplication | null;
  certificateNo: string | null;
  graduatedAt: string | null;
}

// ── notifications ──
export type Audience = 'ALL_STUDENTS' | 'ALL_PROFESSORS' | 'EVERYONE' | 'SECTION';

export interface InboxItem {
  id: number;
  subject: string;
  body: string;
  senderName: string;
  audience: Audience;
  sectionCode: string | null;
  createdAt: string;
  read: boolean;
}

export interface SentNotification {
  id: number;
  audience: Audience;
  sectionCode: string | null;
  subject: string;
  body: string;
  createdAt: string;
  recipientCount: number;
  emailsSent: number;
  emailsPending: number;
  emailsFailed: number;
}

export interface Dashboard {
  pendingStudents: number;
  activeStudents: number;
  activeProfessors: number;
  activeCourses: number;
  pendingGraduations: number;
  failedEmails: number;
  semesters: Semester[];
}

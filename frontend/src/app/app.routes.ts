import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { AuthStore } from './core/auth/auth-store';
import { HOME_BY_ROLE } from './core/auth/models';
import { guestGuard, roleGuard } from './core/guards/role-guard';
import { unsavedChangesGuard } from './core/guards/unsaved-changes-guard';

const shell = () => import('./core/layout/app-shell').then((m) => m.AppShell);
const page = (title: string) => `${title} · Kaka Student Management`;

const accountPage: Routes[number] = {
  path: 'account',
  title: page('Account'),
  canDeactivate: [unsavedChangesGuard],
  loadComponent: () => import('./features/common/account-page').then((m) => m.AccountPage),
};

const professorProfilePage: Routes[number] = {
  path: 'professors/:id',
  title: page('Professor'),
  loadComponent: () =>
    import('./features/common/professor-profile-page').then((m) => m.ProfessorProfilePage),
};

const inboxPage: Routes[number] = {
  path: 'inbox',
  title: page('Inbox'),
  loadComponent: () => import('./features/common/inbox-page').then((m) => m.InboxPage),
};

/**
 * One guarded area per role, each inside the app shell. Every page is loaded only when it is
 * first visited. Forms ask before leaving with unsaved changes.
 */
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: () => {
      const session = inject(AuthStore).snapshot;
      return session ? HOME_BY_ROLE[session.user.role] : '/login';
    },
  },
  {
    path: 'login',
    title: page('Sign in'),
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
  },
  {
    path: 'register',
    title: page('Create an account'),
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register-page').then((m) => m.RegisterPage),
  },
  {
    path: 'admin',
    canActivate: [roleGuard('ADMIN')],
    loadComponent: shell,
    children: [
      {
        path: '',
        title: page('Dashboard'),
        loadComponent: () =>
          import('./features/admin/admin-dashboard').then((m) => m.AdminDashboard),
      },
      {
        path: 'students',
        title: page('Students'),
        loadComponent: () =>
          import('./features/admin/students/students-page').then((m) => m.StudentsPage),
      },
      {
        path: 'students/new',
        title: page('New student'),
        canDeactivate: [unsavedChangesGuard],
        loadComponent: () =>
          import('./features/admin/people/person-form-page').then((m) => m.PersonFormPage),
        data: { kind: 'student' },
      },
      {
        path: 'students/:id',
        title: page('Student'),
        canDeactivate: [unsavedChangesGuard],
        loadComponent: () =>
          import('./features/admin/students/student-detail-page').then((m) => m.StudentDetailPage),
      },
      {
        path: 'professors',
        title: page('Professors'),
        loadComponent: () =>
          import('./features/admin/professors/professors-page').then((m) => m.ProfessorsPage),
      },
      {
        path: 'professors/new',
        title: page('New professor'),
        canDeactivate: [unsavedChangesGuard],
        loadComponent: () =>
          import('./features/admin/people/person-form-page').then((m) => m.PersonFormPage),
        data: { kind: 'professor' },
      },
      {
        path: 'professors/:id',
        title: page('Professor'),
        canDeactivate: [unsavedChangesGuard],
        loadComponent: () =>
          import('./features/admin/professors/professor-detail-page').then(
            (m) => m.ProfessorDetailPage,
          ),
      },
      {
        path: 'courses',
        title: page('Courses'),
        loadComponent: () =>
          import('./features/admin/courses/courses-page').then((m) => m.CoursesPage),
      },
      {
        path: 'courses/new',
        title: page('New course'),
        canDeactivate: [unsavedChangesGuard],
        loadComponent: () =>
          import('./features/admin/courses/course-form-page').then((m) => m.CourseFormPage),
      },
      {
        path: 'courses/:id',
        title: page('Course'),
        loadComponent: () =>
          import('./features/admin/courses/course-detail-page').then((m) => m.CourseDetailPage),
      },
      {
        path: 'courses/:id/edit',
        title: page('Edit course'),
        canDeactivate: [unsavedChangesGuard],
        loadComponent: () =>
          import('./features/admin/courses/course-form-page').then((m) => m.CourseFormPage),
      },
      {
        path: 'semesters',
        title: page('Semesters'),
        canDeactivate: [unsavedChangesGuard],
        loadComponent: () =>
          import('./features/admin/semesters/semesters-page').then((m) => m.SemestersPage),
      },
      {
        path: 'semesters/:id',
        title: page('Semester'),
        canDeactivate: [unsavedChangesGuard],
        loadComponent: () =>
          import('./features/admin/semesters/semester-detail-page').then(
            (m) => m.SemesterDetailPage,
          ),
      },
      {
        path: 'semesters/:semesterId/sections/new',
        title: page('New section'),
        canDeactivate: [unsavedChangesGuard],
        loadComponent: () =>
          import('./features/admin/semesters/section-form-page').then((m) => m.SectionFormPage),
      },
      {
        path: 'sections/:sectionId',
        title: page('Section'),
        canDeactivate: [unsavedChangesGuard],
        loadComponent: () =>
          import('./features/admin/semesters/section-form-page').then((m) => m.SectionFormPage),
      },
      {
        path: 'graduation',
        title: page('Graduation'),
        loadComponent: () =>
          import('./features/admin/graduation-page').then((m) => m.AdminGraduationPage),
      },
      {
        path: 'notifications',
        title: page('Notifications'),
        canDeactivate: [unsavedChangesGuard],
        loadComponent: () =>
          import('./features/admin/notifications-page').then((m) => m.AdminNotificationsPage),
      },
      accountPage,
    ],
  },
  {
    path: 'professor',
    canActivate: [roleGuard('PROFESSOR')],
    loadComponent: shell,
    children: [
      {
        path: '',
        title: page('Teaching'),
        loadComponent: () =>
          import('./features/professor/teaching-page').then((m) => m.TeachingPage),
      },
      {
        path: 'sections/:id',
        title: page('Section'),
        canDeactivate: [unsavedChangesGuard],
        loadComponent: () =>
          import('./features/professor/section-page').then((m) => m.ProfessorSectionPage),
      },
      {
        path: 'profile',
        title: page('My profile'),
        loadComponent: () =>
          import('./features/professor/my-profile-page').then((m) => m.MyProfilePage),
      },
      {
        path: 'profile/edit',
        title: page('Edit profile'),
        canDeactivate: [unsavedChangesGuard],
        loadComponent: () =>
          import('./features/professor/profile-edit-page').then((m) => m.ProfileEditPage),
      },
      {
        path: 'sent',
        title: page('Sent messages'),
        loadComponent: () => import('./features/professor/sent-page').then((m) => m.SentPage),
      },
      inboxPage,
      accountPage,
      professorProfilePage,
    ],
  },
  {
    path: 'student',
    canActivate: [roleGuard('STUDENT')],
    loadComponent: shell,
    children: [
      {
        path: '',
        title: page('Overview'),
        loadComponent: () =>
          import('./features/student/student-dashboard').then((m) => m.StudentDashboard),
      },
      {
        path: 'enroll',
        title: page('Enroll'),
        loadComponent: () => import('./features/student/enroll-page').then((m) => m.EnrollPage),
      },
      {
        path: 'courses',
        title: page('My courses'),
        loadComponent: () =>
          import('./features/student/my-courses-page').then((m) => m.MyCoursesPage),
      },
      {
        path: 'transcript',
        title: page('Transcript'),
        loadComponent: () =>
          import('./features/student/transcript-page').then((m) => m.TranscriptPage),
      },
      {
        path: 'graduation',
        title: page('Graduation'),
        loadComponent: () =>
          import('./features/student/graduation-page').then((m) => m.StudentGraduationPage),
      },
      inboxPage,
      accountPage,
      professorProfilePage,
    ],
  },
  {
    path: '**',
    title: page('Page not found'),
    loadComponent: () => import('./features/not-found/not-found-page').then((m) => m.NotFoundPage),
  },
];

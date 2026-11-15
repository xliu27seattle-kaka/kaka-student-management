import { Role } from '../auth/models';

export interface NavItem {
  label: string;
  icon: string;
  link: string;
  /** Exact match for the area's home, prefix match for everything else. */
  exact?: boolean;
  /** Shows the unread-notifications badge. */
  badge?: 'unread';
}

/** The menu of each role. */
export const NAVIGATION: Record<Role, NavItem[]> = {
  ADMIN: [
    { label: 'Dashboard', icon: 'ph-squares-four', link: '/admin', exact: true },
    { label: 'Students', icon: 'ph-student', link: '/admin/students' },
    { label: 'Professors', icon: 'ph-chalkboard-teacher', link: '/admin/professors' },
    { label: 'Courses', icon: 'ph-books', link: '/admin/courses' },
    { label: 'Semesters', icon: 'ph-calendar-blank', link: '/admin/semesters' },
    { label: 'Graduation', icon: 'ph-graduation-cap', link: '/admin/graduation' },
    { label: 'Notifications', icon: 'ph-megaphone', link: '/admin/notifications' },
    { label: 'Email log', icon: 'ph-envelope-simple', link: '/admin/emails' },
    { label: 'Inbox', icon: 'ph-bell', link: '/admin/inbox', badge: 'unread' },
  ],
  PROFESSOR: [
    { label: 'Teaching', icon: 'ph-squares-four', link: '/professor', exact: true },
    { label: 'My profile', icon: 'ph-identification-card', link: '/professor/profile' },
    { label: 'Sent messages', icon: 'ph-paper-plane-tilt', link: '/professor/sent' },
    { label: 'Inbox', icon: 'ph-bell', link: '/professor/inbox', badge: 'unread' },
  ],
  STUDENT: [
    { label: 'Overview', icon: 'ph-squares-four', link: '/student', exact: true },
    { label: 'Enroll', icon: 'ph-plus-circle', link: '/student/enroll' },
    { label: 'My courses', icon: 'ph-books', link: '/student/courses' },
    { label: 'Transcript', icon: 'ph-scroll', link: '/student/transcript' },
    { label: 'Graduation', icon: 'ph-graduation-cap', link: '/student/graduation' },
    { label: 'Inbox', icon: 'ph-bell', link: '/student/inbox', badge: 'unread' },
  ],
};

export const AREA_LABEL: Record<Role, string> = {
  ADMIN: 'Administration',
  PROFESSOR: 'Faculty',
  STUDENT: 'Student',
};

/**
 * What the portal does for each role, shown on the entry page before anyone signs in.
 * Deliberately generic: no course, section or professor appears before login.
 */
export interface RoleIntro {
  title: string;
  text: string;
  /** Phosphor icon class. */
  icon: string;
}

export const ROLE_INTROS: readonly RoleIntro[] = [
  {
    title: 'Students',
    text: 'Plan each semester, register for sections, and follow your GPA up to graduation.',
    icon: 'ph-student',
  },
  {
    title: 'Professors',
    text: 'See what you teach each semester, grade your classes, and message your students.',
    icon: 'ph-chalkboard-teacher',
  },
  {
    title: 'Administrators',
    text: 'Approve new students, run the semester calendar, and keep the catalogue in order.',
    icon: 'ph-shield-check',
  },
];

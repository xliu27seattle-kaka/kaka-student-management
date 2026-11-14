/** The seeded demo accounts, so a reviewer can sign in with one click. Passwords are public. */
export interface DemoAccount {
  label: string;
  icon: string;
  email: string;
  password: string;
}

export const DEMO_ACCOUNTS: readonly DemoAccount[] = [
  {
    label: 'Admin',
    icon: 'ph-shield-check',
    email: 'admin@kaka.example',
    password: 'KakaDemo2026',
  },
  {
    label: 'Professor',
    icon: 'ph-chalkboard-teacher',
    email: 'amara.okafor@faculty.example',
    password: 'KakaDemo2026',
  },
  {
    label: 'Student',
    icon: 'ph-student',
    email: 'lena.fischer@uni.example',
    password: 'KakaDemo2026',
  },
];

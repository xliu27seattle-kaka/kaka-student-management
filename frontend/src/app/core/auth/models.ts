export type Role = 'ADMIN' | 'PROFESSOR' | 'STUDENT';

export interface UserSummary {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
}

/** What the backend returns after a successful login, and what the app remembers. */
export interface Session {
  accessToken: string;
  /** ISO timestamp after which the token is no longer accepted. */
  expiresAt: string;
  user: UserSummary;
}

export interface RegisterRequest {
  email: string;
  firstName: string;
  lastName: string;
  password: string;
}

export interface RegisterResponse {
  studentNo: string;
  status: 'PENDING';
}

/** Where each role lands after signing in. */
export const HOME_BY_ROLE: Record<Role, string> = {
  ADMIN: '/admin',
  PROFESSOR: '/professor',
  STUDENT: '/student',
};

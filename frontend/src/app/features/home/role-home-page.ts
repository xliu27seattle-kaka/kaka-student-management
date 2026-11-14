import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthStore } from '../../core/auth/auth-store';
import { Role } from '../../core/auth/models';

const AREA_BY_ROLE: Record<Role, string> = {
  ADMIN: 'Administration',
  PROFESSOR: 'Professor workspace',
  STUDENT: 'Student workspace',
};

/**
 * Where a user lands after signing in. This first version shows who is signed in and lets them
 * sign out; the role's real workspace replaces it.
 */
@Component({
  selector: 'app-role-home-page',
  imports: [AsyncPipe],
  templateUrl: './role-home-page.html',
  styleUrl: './role-home-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RoleHomePage {
  private readonly store = inject(AuthStore);
  private readonly router = inject(Router);

  protected readonly user$ = this.store.user$;
  protected readonly areaByRole = AREA_BY_ROLE;

  protected signOut(): void {
    this.store.logout();
    void this.router.navigateByUrl('/login');
  }
}

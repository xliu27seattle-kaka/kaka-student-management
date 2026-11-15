import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toLoadState } from '../../core/api/load-state';
import { ProfessorApi } from '../../shared/api/professor-api';
import { SentList } from '../../shared/ui/sent-list';

@Component({
  selector: 'app-sent-page',
  imports: [AsyncPipe, SentList],
  template: `
    <div class="page" style="max-width: 920px">
      <header class="page-head rise">
        <div>
          <span class="eyebrow">Messages</span>
          <h1>Sent to your classes</h1>
          <p>Write to a class from its section page.</p>
        </div>
      </header>
      <app-sent-list class="rise" style="--i: 1" [state]="(state$ | async)!" />
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SentPage {
  protected readonly state$ = toLoadState(inject(ProfessorApi).sentNotifications());
}

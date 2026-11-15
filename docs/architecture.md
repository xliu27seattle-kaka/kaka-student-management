# Architecture

## The system

```
Browser
  │  HTTPS in production; plain HTTP on localhost
  ▼
frontend               nginx serving the Angular build; forwards /api/ to the backend
  │
  ▼
backend                Spring Boot: REST API, security, every business rule ──► PostgreSQL
  │  RestTemplate, X-Internal-Key header                                          (Flyway migrations)
  ▼
notification-service   Spring Boot: sends one email over SMTP, nothing else
  │
  ├─► local inbox (Mailpit)   by default, and always for reserved example domains
  └─► Gmail SMTP              when credentials are in .env
```

| Service | Port | Notes |
|---|---|---|
| `frontend` | 4200 | Same origin as the API, so no CORS is needed |
| `backend` | 8080 | Swagger UI at `/swagger-ui.html` |
| `notification-service` | none | Reachable only inside the Compose network |
| `db` | 5432 | PostgreSQL 17, named volume |
| `mailbox` | 8025 | Mailpit web inbox |

## Inside the backend

Package by feature, layered inside each feature:

```
com.kakastudio.sms
├── common/         error body and handler, paging, text helpers, configuration
├── security/       JWT issue and check, URL rules per role, current user
├── auth/           login, registration, password change
├── user/ student/ professor/
├── course/         catalogue, PrerequisiteGraph (cycle detection)
├── semester/       lifecycle DRAFT → READY → STARTED → ENDED
├── section/        offerings, MeetingTime (the 10-minute rule)
├── enrollment/     EnrollmentRule classes, the enroll/drop transaction
├── grade/          GradeScale, GpaCalculator, grading by professors
├── transcript/ graduation/
├── notification/   email outbox and RestTemplate gateway; notifications and inboxes
└── dashboard/
```

Each feature has `controller` → `service` (interfaces) → `service.impl` → `repository` / `domain`, plus `dto` records. Controllers only speak DTOs and depend only on service interfaces. Rules that are pure logic live in small classes with no Spring or database in them (`PrerequisiteGraph`, `MeetingTime`, `StandardGradeScale`, `GpaCalculator`, the `EnrollmentRule`s), which is where most unit tests point.

Errors: a service throws a typed exception (`NotFoundException`, `ConflictException`, `RuleViolationException`, `ForbiddenException`) with a code from the one `ErrorCode` enum. `GlobalExceptionHandler` turns it, and every other failure, into the same body:

```json
{ "status": 422, "code": "PREREQUISITE_CYCLE", "message": "These prerequisites would create a cycle: CS101 → CS401 → CS301 → CS201 → CS101", "fieldErrors": [], "timestamp": "…" }
```

## The path of one enrollment

1. The student clicks **Enroll** on `CS301-A`. `OfferingRow` emits `enrolled`; the page calls `EnrollmentStore.enroll()`.
2. The store calls `POST /api/v1/student/enrollments {"sectionId": 42}`. The auth interceptor adds `Authorization: Bearer <jwt>`.
3. nginx forwards `/api/` to the backend. Spring Security checks the token signature and expiry, loads the user from the database (a deactivated account is refused at once), and lets only `STUDENT` into `/api/v1/student/**`.
4. `StudentEnrollmentController` resolves the student from the signed-in user and calls `EnrollmentService.enroll(studentId, sectionId)`.
5. In one transaction, `EnrollmentServiceImpl`:
   - locks the **student** row, then the **section** row (`SELECT … FOR UPDATE`, always in this order, so two requests cannot deadlock);
   - counts the seats taken and loads the student's history;
   - runs every `EnrollmentRule` in order: semester open, account eligible, one section per course, course not already passed, prerequisites passed, seat available, no timetable collision;
   - refuses with the first reason (422 `NOT_ELIGIBLE`, "Section full"), or saves the enrollment (reusing the row if the student had dropped this section).
6. The response comes back; the store reloads the offerings, and the list, the weekly timetable and the credit total all update from the same state.

The student lock stops one student's two simultaneous requests from both passing the timetable check; the section lock stops two students from both taking the last seat.

## The path of one email

1. An action (approving a student, sending a notification, graduating) writes a row to `email_outbox` in its own transaction. If the action rolls back, there is no email.
2. After commit, `EmailDispatchListener` hands the row to `EmailDispatcher`, which calls the notification service through `RestTemplateEmailGateway` (timeouts, internal key header).
3. Success marks the row `SENT` and removes the body (it may contain a temporary password). A temporary failure schedules a retry; a permanent one (rejected address) is `FAILED` at once.
4. `EmailOutboxSweeper` runs every 30 seconds and picks up rows that are due, for example after a restart or while the notification service was down. After `EMAIL_MAX_RETRIES` retries a row is `FAILED` and left alone.

## Inside the frontend

```
src/app
├── core/       auth session store, HTTP interceptors, guards, theme store, confirm and toast stores, app shell
├── shared/     API clients and models, UI pieces (tabs, status badge, empty state, schedule grid,
│               transcript sheet, watermark, professor card, sent list)
└── features/
    ├── auth/       entry page, login, registration
    ├── admin/      dashboard, students, professors, courses, semesters and sections, graduation, notifications
    ├── professor/  teaching, section (roster, grades, message), profile
    ├── student/    overview, enroll, my courses, transcript, graduation
    └── common/     inbox, account, professor profile page
```

- **Routing:** one guarded area per role, each inside the app shell; pages load on first visit; forms are protected by a `canDeactivate` guard that asks through the shared confirmation modal.
- **State:** `BehaviorSubject` stores read with the `async` pipe. Shared state lives in stores (`AuthStore`, `ThemeStore`, `UnreadStore`, `ConfirmStore`, `ToastStore`, and the page-scoped `EnrollmentStore`); page data is a stream built with `toLoadState()`, which gives every page the same loading, data and error states.
- **Themes:** four sets of CSS custom properties on `<html data-theme>`. Graphite & Lime also switches the shell to top navigation. The choice is kept in `localStorage`.
- **Printing:** the transcript and certificate use a layer of real rotated text for the "Kaka Studio" watermark, because browsers drop background images when printing.

## Where each brief item lives

See the table in the [README](../README.md#the-brief-and-where-each-item-lives).

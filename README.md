# Kaka Student Management

A student management system with three roles: administrators, professors and students. It covers the whole registrar workflow: course catalogue with prerequisites, semesters, sections, rule-checked enrollment, grading, GPA, transcripts, graduation and notifications.

Built with Angular 22, Spring Boot 4 (Java 21), PostgreSQL 17 and Docker Compose. A second small Spring Boot service sends email, and the main backend calls it with `RestTemplate`.

<sub>Kaka Studio</sub>

---

## Start it

You only need Docker.

```bash
docker compose up --build
```

The first build takes a few minutes. The ports are bound to this machine only (`127.0.0.1`); if one is already in use, set another in `.env` (for example `DB_PORT=5433`). Then open:

| What | Where |
|---|---|
| The app | http://localhost:4200 |
| Local email inbox (every demo email lands here) | http://localhost:8025 |
| API documentation (Swagger UI) | http://localhost:8080/swagger-ui.html |
| PostgreSQL | `localhost:5432`, database `kaka_sms`, user `kaka`, password `kaka_dev_password` |

No `.env` file is needed: every setting has a working demo default. Copy `.env.example` to `.env` to change ports or to send real email (see [Email](#email)).

To start again from a clean database: `docker compose down -v && docker compose up --build`.

### Demo accounts

Every account uses the password **`KakaDemo2026`**. The login page has one-click buttons for the first three.

| Role | Email | What to look at |
|---|---|---|
| Administrator | `admin@kaka.example` | Dashboard, approvals, catalogue, semesters |
| Professor | `amara.okafor@faculty.example` | Teaching, grading, messaging a class |
| Student | `lena.fischer@uni.example` | Spring 2027 shows every reason a section can be greyed out; CS250-A is open to her |
| Student | `noah.kim@uni.example` | Reaches 31 credits when Fall 2026 ends, then can graduate |
| Student | `sofia.martins@uni.example` | Failed Calculus I and passed the retake; both attempts count in GPA |
| Student | `grace.okonkwo@uni.example` | Already graduated: certificate and official transcript |
| Student | `zara.ahmed@uni.example` | Graduation application waiting for a decision |

Also in the demo data: `omar.haddad@uni.example` is waiting for approval, `ethan.brooks@uni.example` was rejected, `liam.obrien@uni.example` is deactivated, and five more professors have full profiles. All demo addresses use reserved example domains, so nothing is ever sent to a real person.

---

## The brief, and where each item lives

| Requirement | Where to find it |
|---|---|
| CRUD operations | Administrator screens for students, professors, courses, semesters and sections: list, create, view, edit, delete (`/admin/...`). Backend: `*Controller` → `*Service` → `*Repository` in each feature package. |
| Routing | One guarded, lazily loaded area per role (`/admin`, `/professor`, `/student`), detail routes with ids (`/admin/courses/:id`), a not-found page. See `frontend/src/app/app.routes.ts`. |
| Multiple tabs with modal confirmation | Student detail (Profile / Enrollments / Transcript), course detail (Overview / Prerequisites / Sections), professor detail, a professor's section (Roster / Grades / Message). One confirmation modal guards deletes, drops, semester changes, sending messages, and leaving a tab or page with unsaved changes. |
| Error handling | Backend: one `@RestControllerAdvice` returns the same error body everywhere, with a stable `code` from one `ErrorCode` enum. Frontend: an HTTP interceptor signs out on an ended session; forms show field errors from the server; toasts for other failures; every list has loading, empty and error states. |
| Multi-component communication | Inputs and outputs (`OfferingRow` reports enroll, drop and hover to its page), and shared RxJS stores: the enrollment store feeds the section list, the weekly timetable and the credit total at once; the unread store keeps the inbox and the badge in step; the confirm store serves every page's modal. |
| Form handling | Reactive forms with validation: registration, course editor with a prerequisite picker (a custom form control), section editor with a list of weekly meetings, grade entry with a live letter preview, professor profile with lists, notification composer, password change. |
| Backend CRUD services | Spring MVC REST controllers backed by service interfaces and their implementations. |
| Angular 18+ | Angular 22, standalone components, control-flow templates. |
| REST services with Java, Spring, RestTemplate | REST API under `/api/v1`. `RestTemplate` sends every email to the notification service (`RestTemplateEmailGateway`). |
| MySQL/PostgreSQL or H2 | PostgreSQL 17, schema managed by Flyway migrations. |
| RxJS for state | Store services built on `BehaviorSubject`, read with the `async` pipe (`AuthStore`, `ThemeStore`, `EnrollmentStore`, `UnreadStore`, `ConfirmStore`, `ToastStore`). |
| Backend validation, exception handling, transactions | Bean Validation on request records; typed exceptions mapped to 400/401/403/404/409/422; `@Transactional` services. Enrollment is the showcase transaction: it locks the student row, then the section row, and re-checks every rule. |
| Backend unit tests | JUnit 5 and Mockito: 247 backend tests (and 19 in the notification service) on services, rules, the grade scale, GPA, cycle detection, the 10-minute rule and lock order; `@WebMvcTest` slices for validation, security (every role against every area) and error bodies. |
| Public repo, README, runs on localhost | This file; `docker compose up --build`. |

---

## A five-minute tour

1. **Administrator.** Sign in as the admin. The dashboard shows a pending registration and a graduation request. Approve Omar from **Students** (an email appears in the [local inbox](http://localhost:8025)).
2. **Catalogue.** Open **Courses → CS401**. The Prerequisites tab shows what it needs and what it opens up. Edit **CS101** and add CS401 as a prerequisite: the save is refused with the cycle `CS101 → CS401 → CS301 → CS201 → CS101`.
3. **Enrollment.** Sign in as Lena and open **Enroll**. Spring 2027 shows every reason a section can be unavailable: already completed, already taking the course this term, prerequisite missing, section full, overlapping another class, less than 10 minutes after another class, already in another section of the course. Hover a section to see it on the weekly timetable; enroll in CS250-A and watch the timetable and the credits change.
4. **Grading.** Sign in as `amara.okafor@faculty.example` and open **CS320-A** under Fall 2026. The Roster, Grades and Message tabs show the class; the Grades tab previews the letter as you type a score.
5. **End of semester and graduation.** As the admin, open **Semesters → Fall 2026** and click **End the semester**: the confirmation says Lena has no grade in CS301-A yet, and the server refuses. Sign in as `meilin.chen@faculty.example`, give Lena a score in **CS301-A** under Fall 2026, then end the semester as the admin. Sign in as Noah: he now has 31 credits and can apply for graduation. Approve him under **Graduation**, then print his certificate from his Graduation page.
6. **Themes.** Pick another theme in the sidebar: Sage Ledger, Midnight & Amber, Graphite & Lime or Cobalt Trust. Only colours and fonts change; the layout stays the same.

---

## How it is built

```
Browser ──► frontend (nginx + Angular) ──/api──► backend (Spring Boot) ──► PostgreSQL
                                                      │ RestTemplate + internal key
                                                      ▼
                                          notification-service ──► SMTP (local inbox or Gmail)
```

- **Package by feature, layered inside.** `course`, `semester`, `section`, `enrollment`, `grade`, `transcript`, `graduation`, `notification`, ... each with `controller`, `service` (interfaces), `service.impl`, `repository`, `domain`, `dto`.
- **Enrollment rules are small classes behind one interface** (`EnrollmentRule`), run in order. Adding a rule means adding one class.
- **Every email goes through an outbox table** written in the same transaction as the action, then sent after commit and retried by a scheduled job. A failed email never undoes an enrollment.

More in [docs/architecture.md](docs/architecture.md) and the reasoning behind each choice in [docs/decisions.md](docs/decisions.md).

---

## Email

By default every email goes to the local inbox at http://localhost:8025.

To deliver for real through Gmail, put these in `.env` (an [app password](https://support.google.com/accounts/answer/185833) is needed):

```
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_SMTP_AUTH=true
MAIL_STARTTLS=true
MAIL_USERNAME=you@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=you@gmail.com
```

Even then, addresses on reserved domains (`.example`, `.test`, `.invalid`, `.localhost`, `example.com`) still go to the local inbox, so the demo accounts never send real mail. A failed send is retried `EMAIL_MAX_RETRIES` times (default 2) and then given up on.

---

## Query the database from VS Code

Open the folder in VS Code and accept the recommended extensions. **SQLTools** then offers a ready connection called *Kaka SMS (docker compose)* (`.vscode/settings.json`). Useful queries are in [`database/queries/`](database/queries): seats per section, a student's transcript, a GPA check in pure SQL, the prerequisite graph with a recursive query, and the email outbox.

Any other PostgreSQL client works too, with the connection details in [Start it](#start-it).

---

## Tests

Everything runs inside Docker; no local Java or Node is needed for the unit tests.

```bash
scripts/test.sh backend                 # JUnit + Mockito, controller slices
scripts/test.sh notification-service
scripts/test.sh frontend                # Prettier, ESLint, Vitest
scripts/smoke-accounts.sh               # account flows against the running stack (curl)
scripts/smoke-registrar.sh              # catalogue → semester → enrollment → grades → transcript (curl)
scripts/e2e.sh                          # Playwright journeys in Chrome (needs Node and Chrome)
```

Test counts: backend 247, notification service 19, frontend 47 (Vitest), Playwright 43, smoke scripts 23 + 32 checks.

The Playwright tests and smoke scripts add their own courses, semesters and students with random names, so they can run again and again. Some journeys read the demo semesters (Fall 2026 running, Spring 2027 open), so run them on demo data that has not been walked through the tour: reset with `docker compose down -v && docker compose up --build`.

GitHub Actions runs the formatting checks and unit tests of all three modules on every push.

---

## Project layout

```
backend/               Spring Boot API, Flyway migrations and demo data
notification-service/  Spring Boot email sender
frontend/              Angular app and its nginx configuration
e2e/                   Playwright end-to-end tests
database/queries/      SQL to run from VS Code
docs/                  architecture and decisions
scripts/               test, smoke and format helpers
docker-compose.yml     the whole stack
```

---

## Not production-ready yet

This is a demo. Before real use it would need:

- **Real secrets.** The JWT secret, internal API key and database password have public demo defaults, and the administrator account (`admin@kaka.example`) is created by a migration with a published password.
- **HTTPS.** Everything runs over plain HTTP on localhost.
- **Rate limiting** on login and registration, and account lockout after repeated failures.
- **Password reset by email** and refresh tokens; today a session simply expires.
- **Token storage.** The access token is kept in `localStorage`; an HttpOnly cookie (with CSRF protection) is safer against XSS.
- **Service-to-service security** stronger than a shared key, such as mutual TLS.
- **Email dispatch outside the database transaction.** Today the outbox row stays locked while the notification service is called (up to 12 seconds); a busy system would claim rows with `FOR UPDATE SKIP LOCKED`, call the service with no transaction open, and send the outbox id as an idempotency key.
- **Monitoring** of the email outbox (rows that were given up on are only counted on the dashboard).
- **Integration tests** against a real PostgreSQL in CI (Testcontainers), including concurrent enrollment for the last seat.

---

## Credits

Landing page photographs, all public domain or CC0, from Wikimedia Commons: Cherry blossoms at the Tidal Basin by Carol M. Highsmith; Great Falls of the Potomac, Assateague Island and Hog Rock Overlook (Catoctin Mountain Park) by the National Park Service; C&O Canal towpath by Bonnachoven. Sources are listed in `frontend/public/entry/photo-credits.json`. Professor portraits are illustrations drawn for this project. Fonts: Manrope, Geist, Geist Mono and Poppins (SIL Open Font License). Icons: Phosphor.

Made by Kaka Studio.

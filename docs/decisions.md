# Decisions

The main choices, what else was considered, and why. Each ends with how it would change if the system grew.

## 1. A small notification service called with RestTemplate

**Chosen:** the main backend owns every rule and the database; a second, tiny Spring Boot service only sends email, and the backend calls it over HTTP with `RestTemplate`.

**Considered:** one backend that sends mail itself (simplest, but `RestTemplate`, which the brief names, would have no real job); one microservice per domain (rejected: enrollment must check capacity, prerequisites and the timetable atomically, which is one transaction in one database).

**Why:** sending mail is slow and can fail, so it is isolated, and a failed email never fails an enrollment. **If it grew:** SMS or push would be another channel inside the notification service; a message broker could replace the HTTP call behind the `EmailGateway` interface.

## 2. A transactional outbox instead of a message queue

**Chosen:** every email is first a row in `email_outbox`, written in the same transaction as the action; it is sent after commit, and a scheduled job retries what failed (`EMAIL_MAX_RETRIES`, default 2), then gives up.

**Considered:** Kafka. It does not remove the need for the outbox (the database write and the publish are still two systems), it takes `RestTemplate` out of the picture, and it is a heavy container for a handful of emails.

**Why:** no email is lost when the notification service is down, and no email goes out for an action that rolled back. **If it grew:** the sweeper would hand rows to a broker instead of calling the service directly.

## 3. Enrollment rules as small classes behind one interface

**Chosen:** `EnrollmentRule` with one implementation per rule, run in `@Order`. The listing endpoint runs them to explain greyed-out sections; enrolling runs them again inside the transaction.

**Why:** each rule is tested on its own, the reasons shown to the student come from the same code that enforces them, and a new rule is one new class. **If it grew:** a per-semester credit limit would be one more class.

## 4. Pessimistic locks, student first, then section

**Chosen:** `SELECT … FOR UPDATE` on the student row, then on the section row.

**Considered:** optimistic locking with a version column (needs a retry loop in the client for a common case, the popular section); a database constraint on capacity (cannot express "count of enrolled rows ≤ capacity").

**Why:** the section lock makes the last seat safe; the student lock makes two simultaneous requests of one student see each other for the timetable check; the fixed order prevents deadlocks. **If it grew:** for very busy sections, a seat counter column updated in the same transaction would shorten the lock.

## 5. The 10-minute rule in one place

Two meetings on the same day collide unless there are at least 10 minutes between the end of one and the start of the other. `MeetingTime.collidesWith` is used for a section's own meetings, for a professor's week and for a student's week, so they can never disagree. Times are compared as minutes since midnight, so a late class never wraps to the next morning.

## 6. Prerequisite cycles found with depth-first search

On every change, `PrerequisiteGraph` follows the proposed prerequisites depth first; if it reaches the course again, the change is refused and the path is shown (`CS101 → CS401 → CS301 → CS201 → CS101`). A course can never be its own prerequisite. Changes affect future enrollments only.

## 7. Grades: one replaceable scale, GPA computed, not stored

`GradeScale` turns a score into a letter and points; `StandardGradeScale` is the usual US scale. GPA is computed from the enrollments each time (F counts as 0, W is left out, only ended semesters count), so it can never drift from the grades. Sections copy the course's credits when created, so editing a course later never rewrites a transcript.

## 8. Semesters move only by an administrator's action

Status never changes by date. Going back to draft needs nobody enrolled; ending needs every enrolled student graded and turns their enrollments into completed ones in the same transaction. Prerequisites count only once a semester has ended, so the working order is: end the current semester, then let students enroll for the next.

## 9. JWT checked against the database on every request

**Chosen:** a signed HS256 token, and on each request the user is loaded to read the current role and status.

**Why:** deactivating an account takes effect at once, not when the token expires. The cost is one indexed query per request. **If it grew:** a short-lived cache, or refresh tokens with a revocation list.

## 10. Roles by URL prefix, ownership in the services

`/api/v1/admin/**`, `/api/v1/professor/**` and `/api/v1/student/**` each admit one role. "Only your own" (a professor grades only their own sections, a student drops only their own seats) is checked in the service, because only the service knows who owns what.

## 11. PostgreSQL, not H2

The same database in development and production, so locks, constraints and SQL behave the same; data survives restarts; any standard client (VS Code, psql) can connect. H2 would be the choice for fast throwaway tests, and Testcontainers would be the choice for integration tests.

## 12. Angular with RxJS stores, no component library

State lives in `BehaviorSubject` stores read with the `async` pipe, which is easy to follow and test. UI pieces are written for this app (modal on the native `<dialog>`, tabs, badges), so there is nothing to explain that a library would hide. The four themes are only CSS custom properties.

## 13. Demo data in a repeatable Flyway script

The schema migrations and the demo data live in separate Flyway locations; `APP_DEMO_DATA=false` starts an empty system. The demo script loads once and covers every state the app has, so a reviewer sees it all in a few minutes.

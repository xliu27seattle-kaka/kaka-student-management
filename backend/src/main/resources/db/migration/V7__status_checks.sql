-- The allowed values of every status column, enforced by the database as well as by the enums.

alter table users add constraint users_role_check check (role in ('ADMIN', 'PROFESSOR', 'STUDENT'));
alter table users add constraint users_status_check
  check (status in ('PENDING', 'ACTIVE', 'REJECTED', 'INACTIVE'));
alter table students add constraint students_academic_status_check
  check (academic_status in ('ENROLLED', 'GRADUATED'));
alter table semesters add constraint semesters_status_check
  check (status in ('DRAFT', 'READY', 'STARTED', 'ENDED'));
alter table enrollments add constraint enrollments_status_check
  check (status in ('ENROLLED', 'DROPPED', 'WITHDRAWN', 'COMPLETED'));
alter table graduation_applications add constraint graduation_applications_status_check
  check (status in ('PENDING', 'APPROVED', 'REJECTED'));
alter table notifications add constraint notifications_audience_check
  check (audience in ('ALL_STUDENTS', 'ALL_PROFESSORS', 'EVERYONE', 'SECTION'));
alter table email_outbox add constraint email_outbox_status_check
  check (status in ('PENDING', 'SENT', 'FAILED'));

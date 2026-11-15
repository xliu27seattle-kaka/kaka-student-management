-- Notes the system writes itself (account approved, graduation decided, section assigned, alerts
-- for administrators) have no sender and the audience PERSONAL.

alter table notifications alter column sender_id drop not null;
alter table notifications drop constraint notifications_audience_check;
alter table notifications add constraint notifications_audience_check
  check (audience in ('ALL_STUDENTS', 'ALL_PROFESSORS', 'EVERYONE', 'SECTION', 'PERSONAL'));

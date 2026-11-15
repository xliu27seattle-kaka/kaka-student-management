-- Graduation: an application per attempt, and the certificate on the student once approved.

alter table students add column certificate_no varchar(16) unique;
alter table students add column graduated_at timestamptz;

create sequence certificate_no_seq start 1;

create table graduation_applications (
  id         bigserial primary key,
  student_id bigint      not null references students (id),
  status     varchar(10) not null,                  -- PENDING | APPROVED | REJECTED
  applied_at timestamptz not null default now(),
  decided_at timestamptz
);

-- At most one open application per student.
create unique index graduation_applications_one_pending
  on graduation_applications (student_id) where status = 'PENDING';

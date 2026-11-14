-- Accounts: one row per person in users, plus a role-specific row.

create table users (
  id            bigserial primary key,
  email         varchar(255) not null unique,   -- stored trimmed and lower-cased
  password_hash varchar(100) not null,          -- BCrypt
  first_name    varchar(80)  not null,
  last_name     varchar(80)  not null,
  role          varchar(20)  not null,          -- ADMIN | PROFESSOR | STUDENT
  status        varchar(20)  not null,          -- PENDING | ACTIVE | REJECTED | INACTIVE
  created_at    timestamptz  not null default now(),
  updated_at    timestamptz  not null default now()
);

create table students (
  id              bigserial primary key,
  user_id         bigint      not null unique references users (id),
  student_no      varchar(12) not null unique,                 -- e.g. S2026-0001
  academic_status varchar(20) not null default 'ENROLLED'      -- ENROLLED | GRADUATED
);

create table professors (
  id           bigserial primary key,
  user_id      bigint      not null unique references users (id),
  professor_no varchar(10) not null unique,                    -- e.g. P-0001
  title        varchar(120),
  bio          text,
  photo_path   varchar(255)
);

create table professor_expertise (
  professor_id bigint       not null references professors (id) on delete cascade,
  position     int          not null,
  label        varchar(120) not null,
  primary key (professor_id, position)
);

create table professor_experience (
  professor_id bigint       not null references professors (id) on delete cascade,
  position     int          not null,
  role_title   varchar(160) not null,
  organization varchar(160) not null,
  period       varchar(60)  not null,
  primary key (professor_id, position)
);

create table professor_awards (
  professor_id bigint       not null references professors (id) on delete cascade,
  position     int          not null,
  title        varchar(200) not null,
  award_year   int          not null,
  primary key (professor_id, position)
);

create sequence student_no_seq start 1;
create sequence professor_no_seq start 1;

-- Every email is written here in the same transaction as the action that caused it,
-- then sent after commit and retried if the send fails.
create table email_outbox (
  id              bigserial primary key,
  recipient       varchar(255) not null,
  subject         varchar(200) not null,
  html            text         not null,
  status          varchar(20)  not null,          -- PENDING | SENT | FAILED
  attempts        int          not null default 0,
  last_error      varchar(500),
  next_attempt_at timestamptz  not null default now(),
  created_at      timestamptz  not null default now(),
  sent_at         timestamptz
);

create index email_outbox_due on email_outbox (status, next_attempt_at);

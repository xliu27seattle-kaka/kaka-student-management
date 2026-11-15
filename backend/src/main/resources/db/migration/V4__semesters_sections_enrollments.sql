-- Semesters, the sections offered in them, and who is enrolled where.

create table semesters (
  id         bigserial primary key,
  code       varchar(8)  not null unique,           -- e.g. 2027SP
  name       varchar(60) not null,                  -- e.g. Spring 2027
  start_date date        not null,
  end_date   date        not null,
  status     varchar(10) not null default 'DRAFT',  -- DRAFT | READY | STARTED | ENDED
  check (end_date > start_date)
);

create table sections (
  id           bigserial primary key,
  course_id    bigint      not null references courses (id),
  semester_id  bigint      not null references semesters (id),
  professor_id bigint      not null references professors (id),
  letter       varchar(2)  not null,                -- A, B, ... unique per course and semester
  capacity     int         not null default 20 check (capacity between 1 and 200),
  room         varchar(40),
  credits      int         not null,                -- copied from the course when created
  unique (course_id, semester_id, letter)
);

create index sections_semester on sections (semester_id);
create index sections_professor on sections (professor_id, semester_id);

create table section_meetings (
  section_id  bigint      not null references sections (id) on delete cascade,
  position    int         not null,
  day_of_week varchar(10) not null,                 -- MONDAY ... SUNDAY
  start_time  time        not null,
  end_time    time        not null,
  primary key (section_id, position),
  check (end_time > start_time)
);

create table enrollments (
  id            bigserial primary key,
  student_id    bigint       not null references students (id),
  section_id    bigint       not null references sections (id) on delete cascade,
  status        varchar(10)  not null,              -- ENROLLED | DROPPED | WITHDRAWN | COMPLETED
  score         int          check (score between 0 and 100),
  letter_grade  varchar(2),
  grade_points  numeric(2,1),
  enrolled_at   timestamptz  not null default now(),
  updated_at    timestamptz  not null default now(),
  unique (student_id, section_id)
);

create index enrollments_section on enrollments (section_id, status);

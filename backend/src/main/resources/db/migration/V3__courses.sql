-- The course catalogue. A course is what is taught; a section (V4) is one offering of it.

create table courses (
  id          bigserial primary key,
  code        varchar(10)  not null unique,         -- e.g. CS201, stored upper case
  title       varchar(120) not null,
  description text,
  department  varchar(6)   not null,                -- e.g. CS
  credits     int          not null default 3 check (credits between 1 and 6),
  active      boolean      not null default true    -- a retired course gets no new sections
);

-- course_id requires prerequisite_id. Cycles are refused by the service before a write.
create table course_prerequisites (
  course_id       bigint not null references courses (id) on delete cascade,
  prerequisite_id bigint not null references courses (id),
  primary key (course_id, prerequisite_id),
  check (course_id <> prerequisite_id)
);

create index course_prerequisites_prerequisite on course_prerequisites (prerequisite_id);

-- Demo accounts for trying the app. Loaded unless APP_DEMO_DATA=false.
-- Repeatable and safe to re-run: an account that already exists is left alone.
-- Password for both: KakaDemo2026 (same BCrypt hash as the administrator).

with new_user as (
  insert into users (email, password_hash, first_name, last_name, role, status)
  values ('amara.okafor@faculty.example', '$2y$10$xbk5lVSHxSkdG5SmcJTjhuVM/bHaTTH0otH7e8tXVtddm0FlrDSim', 'Amara', 'Okafor', 'PROFESSOR', 'ACTIVE')
  on conflict (email) do nothing
  returning id
), new_professor as (
  insert into professors (user_id, professor_no, title, bio)
  select id,
         'P-' || lpad(nextval('professor_no_seq')::text, 4, '0'),
         'Associate Professor of Computer Science',
         'I teach databases and distributed systems, and I like courses where every week ends with something that runs.'
  from new_user
  returning id
), expertise as (
  insert into professor_expertise (professor_id, position, label)
  select id, 0, 'Database systems' from new_professor
  union all select id, 1, 'Distributed systems' from new_professor
  union all select id, 2, 'Data modelling' from new_professor
), experience as (
  insert into professor_experience (professor_id, position, role_title, organization, period)
  select id, 0, 'Senior Data Engineer', 'Northwind Data', '2012 to 2018' from new_professor
)
insert into professor_awards (professor_id, position, title, award_year)
select id, 0, 'Teaching Excellence Award', 2023 from new_professor;

with new_user as (
  insert into users (email, password_hash, first_name, last_name, role, status)
  values ('lena.fischer@uni.example', '$2y$10$xbk5lVSHxSkdG5SmcJTjhuVM/bHaTTH0otH7e8tXVtddm0FlrDSim', 'Lena', 'Fischer', 'STUDENT', 'ACTIVE')
  on conflict (email) do nothing
  returning id
)
insert into students (user_id, student_no)
select id, 'S2026-' || lpad(nextval('student_no_seq')::text, 4, '0') from new_user;

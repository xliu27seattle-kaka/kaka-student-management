-- Demo data: a registrar in full swing. Loaded unless APP_DEMO_DATA=false, after R__demo_accounts.
-- Runs only once: if semester 2025FA exists, nothing happens. Every password is KakaDemo2026.
--
-- What it sets up (see README, "Demo walkthrough"):
--   Fall 2025 and Spring 2026 ENDED with grades, Fall 2026 STARTED (all graded but Lena in CS301-A),
--   Spring 2027 READY (enrollment open), Fall 2027 DRAFT.
--   lena.fischer   sees every greyed-out reason on Spring 2027, and can take CS250-A
--   noah.kim       reaches 31 credits when Fall 2026 ends: grade, end the semester, graduate
--   sofia.martins  failed MATH140 and passed it on the retake (both attempts count in GPA);
--                  withdrew from CS220 (W, not in GPA)
--   grace.okonkwo  graduated (certificate KS-2026-0001); zara.ahmed has a pending application
--   omar.haddad    waits for approval; ethan.brooks was rejected; liam.obrien is deactivated

-- ---------- helpers, alive only for this session ----------

create or replace function pg_temp.demo_hash() returns text language sql as
$$ select '$2y$10$xbk5lVSHxSkdG5SmcJTjhuVM/bHaTTH0otH7e8tXVtddm0FlrDSim'::text $$;

create or replace function pg_temp.add_user(p_email text, p_first text, p_last text, p_role text, p_status text, p_created timestamptz)
returns bigint language plpgsql as $$
declare v_id bigint;
begin
  select id into v_id from users where email = p_email;
  if v_id is null then
    insert into users (email, password_hash, first_name, last_name, role, status, created_at, updated_at)
    values (p_email, pg_temp.demo_hash(), p_first, p_last, p_role, p_status, p_created, p_created)
    returning id into v_id;
  end if;
  return v_id;
end $$;

create or replace function pg_temp.add_professor(p_email text, p_first text, p_last text, p_title text, p_bio text, p_photo text, p_expertise text[])
returns bigint language plpgsql as $$
declare v_user bigint; v_id bigint; i int;
begin
  v_user := pg_temp.add_user(p_email, p_first, p_last, 'PROFESSOR', 'ACTIVE', timestamptz '2025-06-01 09:00+00');
  select id into v_id from professors where user_id = v_user;
  if v_id is null then
    insert into professors (user_id, professor_no) values (v_user, 'P-' || lpad(nextval('professor_no_seq')::text, 4, '0'))
    returning id into v_id;
  end if;
  update professors set title = p_title, bio = p_bio, photo_path = p_photo where id = v_id;
  delete from professor_expertise where professor_id = v_id;
  for i in 1 .. coalesce(array_length(p_expertise, 1), 0) loop
    insert into professor_expertise (professor_id, position, label) values (v_id, i - 1, p_expertise[i]);
  end loop;
  return v_id;
end $$;

create or replace function pg_temp.add_experience(p_email text, p_position int, p_role text, p_org text, p_period text)
returns void language sql as $$
  insert into professor_experience (professor_id, position, role_title, organization, period)
  select p.id, p_position, p_role, p_org, p_period from professors p join users u on u.id = p.user_id where u.email = p_email
  on conflict (professor_id, position) do update set role_title = excluded.role_title, organization = excluded.organization, period = excluded.period
$$;

create or replace function pg_temp.add_award(p_email text, p_position int, p_title text, p_year int)
returns void language sql as $$
  insert into professor_awards (professor_id, position, title, award_year)
  select p.id, p_position, p_title, p_year from professors p join users u on u.id = p.user_id where u.email = p_email
  on conflict (professor_id, position) do update set title = excluded.title, award_year = excluded.award_year
$$;

create or replace function pg_temp.add_student(p_email text, p_first text, p_last text, p_status text, p_year int, p_created timestamptz)
returns bigint language plpgsql as $$
declare v_user bigint; v_id bigint;
begin
  v_user := pg_temp.add_user(p_email, p_first, p_last, 'STUDENT', p_status, p_created);
  select id into v_id from students where user_id = v_user;
  if v_id is null then
    insert into students (user_id, student_no)
    values (v_user, 'S' || p_year || '-' || lpad(nextval('student_no_seq')::text, 4, '0'))
    returning id into v_id;
  end if;
  return v_id;
end $$;

create or replace function pg_temp.add_course(p_code text, p_title text, p_dept text, p_credits int, p_description text, p_prereqs text[], p_active boolean default true)
returns bigint language plpgsql as $$
declare v_id bigint;
begin
  insert into courses (code, title, department, credits, description, active)
  values (p_code, p_title, p_dept, p_credits, p_description, p_active)
  returning id into v_id;
  insert into course_prerequisites (course_id, prerequisite_id)
  select v_id, c.id from courses c where c.code = any (p_prereqs);
  return v_id;
end $$;

create or replace function pg_temp.add_semester(p_code text, p_name text, p_start date, p_end date, p_status text)
returns bigint language sql as $$
  insert into semesters (code, name, start_date, end_date, status) values (p_code, p_name, p_start, p_end, p_status) returning id
$$;

-- p_meetings looks like 'MONDAY 10:00-11:15, WEDNESDAY 10:00-11:15'
create or replace function pg_temp.add_section(p_course text, p_semester text, p_professor text, p_letter text, p_capacity int, p_room text, p_meetings text)
returns bigint language plpgsql as $$
declare v_id bigint; v_slot text; v_pos int := 0;
begin
  insert into sections (course_id, semester_id, professor_id, letter, capacity, room, credits)
  select c.id, s.id, p.id, p_letter, p_capacity, p_room, c.credits
  from courses c, semesters s, professors p join users u on u.id = p.user_id
  where c.code = p_course and s.code = p_semester and u.email = p_professor
  returning id into v_id;
  foreach v_slot in array string_to_array(p_meetings, ', ') loop
    insert into section_meetings (section_id, position, day_of_week, start_time, end_time)
    values (v_id, v_pos, split_part(v_slot, ' ', 1),
            split_part(split_part(v_slot, ' ', 2), '-', 1)::time,
            split_part(split_part(v_slot, ' ', 2), '-', 2)::time);
    v_pos := v_pos + 1;
  end loop;
  return v_id;
end $$;

-- The grade scale of StandardGradeScale, for seeded scores.
create or replace function pg_temp.letter(p_score int) returns text language sql as $$
  select case when p_score >= 93 then 'A' when p_score >= 90 then 'A-' when p_score >= 87 then 'B+'
              when p_score >= 83 then 'B' when p_score >= 80 then 'B-' when p_score >= 77 then 'C+'
              when p_score >= 73 then 'C' when p_score >= 70 then 'C-' when p_score >= 60 then 'D' else 'F' end
$$;

create or replace function pg_temp.points(p_score int) returns numeric language sql as $$
  select case when p_score >= 93 then 4.0 when p_score >= 90 then 3.7 when p_score >= 87 then 3.3
              when p_score >= 83 then 3.0 when p_score >= 80 then 2.7 when p_score >= 77 then 2.3
              when p_score >= 73 then 2.0 when p_score >= 70 then 1.7 when p_score >= 60 then 1.0 else 0.0 end
$$;

-- p_section is like 'CS201-A@2026SP'
create or replace function pg_temp.enroll(p_student text, p_section text, p_status text, p_score int default null)
returns void language sql as $$
  insert into enrollments (student_id, section_id, status, score, letter_grade, grade_points, enrolled_at, updated_at)
  select st.id, se.id, p_status, p_score,
         case when p_score is null then null else pg_temp.letter(p_score) end,
         case when p_score is null then null else pg_temp.points(p_score) end,
         sem.start_date - 14, sem.start_date - 14
  from students st join users u on u.id = st.user_id,
       sections se join courses c on c.id = se.course_id join semesters sem on sem.id = se.semester_id
  where u.email = p_student
    and c.code = split_part(split_part(p_section, '@', 1), '-', 1)
    and se.letter = split_part(split_part(p_section, '@', 1), '-', 2)
    and sem.code = split_part(p_section, '@', 2)
$$;

create or replace function pg_temp.notify(p_sender text, p_audience text, p_section text, p_subject text, p_body text, p_at timestamptz, p_to text[], p_read text[])
returns void language plpgsql as $$
declare v_id bigint; v_section bigint; v_email text; v_user bigint; v_outbox bigint;
begin
  if p_section is not null then
    select se.id into v_section
    from sections se join courses c on c.id = se.course_id join semesters sem on sem.id = se.semester_id
    where c.code = split_part(split_part(p_section, '@', 1), '-', 1)
      and se.letter = split_part(split_part(p_section, '@', 1), '-', 2)
      and sem.code = split_part(p_section, '@', 2);
  end if;
  insert into notifications (sender_id, audience, section_id, subject, body, recipient_count, created_at)
  select u.id, p_audience, v_section, p_subject, p_body, array_length(p_to, 1), p_at from users u where u.email = p_sender
  returning id into v_id;
  foreach v_email in array p_to loop
    select id into v_user from users where email = v_email;
    insert into email_outbox (recipient, subject, html, status, attempts, created_at, sent_at, next_attempt_at)
    values (v_email, p_subject, '(removed)', 'SENT', 1, p_at, p_at, p_at)
    returning id into v_outbox;
    insert into notification_recipients (notification_id, user_id, read_at, email_outbox_id)
    values (v_id, v_user, case when v_email = any (p_read) then p_at + interval '3 hours' end, v_outbox);
  end loop;
end $$;

-- A note the system wrote to one person (no sender), optionally with the email it sent.
create or replace function pg_temp.system_note(p_to text, p_subject text, p_body text, p_at timestamptz, p_emailed boolean, p_read boolean)
returns void language plpgsql as $$
declare v_id bigint; v_user bigint; v_outbox bigint;
begin
  select id into v_user from users where email = p_to;
  insert into notifications (sender_id, audience, subject, body, recipient_count, created_at)
  values (null, 'PERSONAL', p_subject, p_body, 1, p_at) returning id into v_id;
  if p_emailed then
    insert into email_outbox (recipient, subject, html, status, attempts, created_at, sent_at, next_attempt_at)
    values (p_to, p_subject, '(removed)', 'SENT', 1, p_at, p_at, p_at) returning id into v_outbox;
  end if;
  insert into notification_recipients (notification_id, user_id, read_at, email_outbox_id)
  values (v_id, v_user, case when p_read then p_at + interval '1 hour' end, v_outbox);
end $$;

-- ---------- the data ----------

do $$
begin
  if exists (select 1 from semesters where code = '2025FA') then
    return;
  end if;

  -- professors (Amara Okafor already exists from R__demo_accounts; this fills in her profile)
  perform pg_temp.add_professor('amara.okafor@faculty.example', 'Amara', 'Okafor',
    'Associate Professor of Computer Science',
    'I teach databases and distributed systems, and I like courses where every week ends with something that runs. Before academia I spent six years building data pipelines, so my assignments look a lot like real tickets.',
    'okafor.svg', array['Database systems', 'Distributed systems', 'Data modelling']);
  perform pg_temp.add_professor('daniel.reyes@faculty.example', 'Daniel', 'Reyes',
    'Professor of Mathematics',
    'Mathematics is a language for being precise about ideas. In my classes we prove things together on the board, slowly, until the proof feels obvious.',
    'reyes.svg', array['Discrete mathematics', 'Calculus', 'Number theory']);
  perform pg_temp.add_professor('meilin.chen@faculty.example', 'Mei Lin', 'Chen',
    'Assistant Professor of Computer Science',
    'I work on algorithms for large graphs. My favourite moment in teaching is when a student realises that a clever data structure turned a slow program into an instant one.',
    'chen.svg', array['Algorithms', 'Graph theory', 'Competitive programming']);
  perform pg_temp.add_professor('samuel.adeyemi@faculty.example', 'Samuel', 'Adeyemi',
    'Senior Lecturer in Physics',
    'Physics is the study of what happens when you take simple rules seriously. Expect demonstrations, back-of-the-envelope estimates and a lot of questions.',
    'adeyemi.svg', array['Classical mechanics', 'Physics education']);
  perform pg_temp.add_professor('hannah.novak@faculty.example', 'Hannah', 'Novak',
    'Lecturer in Academic Writing',
    'Clear writing is clear thinking made visible. I help scientists and engineers say exactly what they mean, in fewer words.',
    'novak.svg', array['Academic writing', 'Technical communication', 'Rhetoric']);
  perform pg_temp.add_professor('priya.raman@faculty.example', 'Priya', 'Raman',
    'Associate Professor of Statistics',
    'Data rarely lies, but it often misleads. My courses are about asking the right question of a data set before reaching for a model.',
    'raman.svg', array['Statistical inference', 'Data science', 'Experimental design']);

  perform pg_temp.add_experience('amara.okafor@faculty.example', 1, 'Visiting Researcher', 'Lakeside Systems Lab', '2019 to 2020');
  perform pg_temp.add_experience('daniel.reyes@faculty.example', 0, 'Chair of Mathematics', 'Kaka Student Management', '2021 to now');
  perform pg_temp.add_experience('daniel.reyes@faculty.example', 1, 'Postdoctoral Fellow', 'Institute for Pure Mathematics', '2008 to 2011');
  perform pg_temp.add_experience('meilin.chen@faculty.example', 0, 'Software Engineer', 'Graphwise', '2016 to 2019');
  perform pg_temp.add_experience('samuel.adeyemi@faculty.example', 0, 'Science Outreach Lead', 'City Science Museum', '2012 to 2017');
  perform pg_temp.add_experience('hannah.novak@faculty.example', 0, 'Editor', 'Northbank Press', '2010 to 2016');
  perform pg_temp.add_experience('priya.raman@faculty.example', 0, 'Data Scientist', 'Harbor Health Analytics', '2013 to 2018');
  perform pg_temp.add_award('daniel.reyes@faculty.example', 0, 'Distinguished Teaching Award', 2022);
  perform pg_temp.add_award('meilin.chen@faculty.example', 0, 'Early Career Research Grant', 2024);
  perform pg_temp.add_award('samuel.adeyemi@faculty.example', 0, 'Public Engagement Prize', 2019);
  perform pg_temp.add_award('priya.raman@faculty.example', 0, 'Best Paper, Applied Statistics Conference', 2021);
  perform pg_temp.add_award('priya.raman@faculty.example', 1, 'Mentor of the Year', 2024);

  -- courses: chains up to four levels (CS101 > CS201 > CS301 > CS401)
  perform pg_temp.add_course('CS101', 'Introduction to Programming', 'CS', 3, 'Variables, control flow, functions and testing, in Python. Weekly labs build up to a small game.', array[]::text[]);
  perform pg_temp.add_course('MATH120', 'Discrete Mathematics', 'MATH', 3, 'Logic, sets, proofs, counting and graphs: the mathematics of computer science.', array[]::text[]);
  perform pg_temp.add_course('MATH140', 'Calculus I', 'MATH', 4, 'Limits, derivatives and integrals, with applications to motion and growth.', array[]::text[]);
  perform pg_temp.add_course('ENG110', 'Academic Writing', 'ENG', 3, 'Planning, drafting and revising arguments; citing sources; writing for a technical audience.', array[]::text[]);
  perform pg_temp.add_course('CS150', 'Ethics in Computing', 'CS', 3, 'Privacy, bias and responsibility in software. Retired in 2026; its topics moved into CS340.', array[]::text[], false);
  perform pg_temp.add_course('PHYS110', 'Physics for Scientists', 'PHYS', 4, 'Mechanics, energy and waves, with a weekly lab.', array['MATH140']);
  perform pg_temp.add_course('STAT200', 'Probability and Statistics', 'STAT', 3, 'Random variables, distributions, estimation and hypothesis tests.', array['MATH140']);
  perform pg_temp.add_course('CS201', 'Data Structures', 'CS', 3, 'Lists, trees, hash tables and graphs, and how to choose between them.', array['CS101', 'MATH120']);
  perform pg_temp.add_course('CS220', 'Computer Systems', 'CS', 3, 'How programs really run: memory, processes, files and the operating system.', array['CS101']);
  perform pg_temp.add_course('CS240', 'Web Development', 'CS', 3, 'HTTP, HTML, CSS and a modern front-end framework, ending with a deployed project.', array['CS101']);
  perform pg_temp.add_course('CS250', 'Human-Computer Interaction', 'CS', 3, 'Designing software people can use: user research, prototyping and usability testing.', array['CS101']);
  perform pg_temp.add_course('CS301', 'Algorithms', 'CS', 3, 'Divide and conquer, dynamic programming, greedy methods and graph algorithms.', array['CS201']);
  perform pg_temp.add_course('CS320', 'Database Systems', 'CS', 3, 'Relational design, SQL, transactions and indexing.', array['CS201']);
  perform pg_temp.add_course('CS340', 'Software Engineering', 'CS', 3, 'Requirements, design, testing and teamwork on a semester-long project.', array['CS201', 'ENG110']);
  perform pg_temp.add_course('STAT310', 'Data Science', 'STAT', 3, 'From raw data to a defensible conclusion: cleaning, modelling and communicating.', array['STAT200', 'CS201']);
  perform pg_temp.add_course('CS401', 'Distributed Systems', 'CS', 3, 'Replication, consensus and failure, with a project on a small cluster.', array['CS301', 'CS320']);

  -- semesters
  perform pg_temp.add_semester('2025FA', 'Fall 2025', date '2025-08-25', date '2025-12-12', 'ENDED');
  perform pg_temp.add_semester('2026SP', 'Spring 2026', date '2026-01-12', date '2026-05-08', 'ENDED');
  perform pg_temp.add_semester('2026FA', 'Fall 2026', date '2026-08-24', date '2026-12-11', 'STARTED');
  perform pg_temp.add_semester('2027SP', 'Spring 2027', date '2027-01-11', date '2027-05-07', 'READY');
  perform pg_temp.add_semester('2027FA', 'Fall 2027', date '2027-08-23', date '2027-12-10', 'DRAFT');

  -- Fall 2025
  perform pg_temp.add_section('CS101', '2025FA', 'amara.okafor@faculty.example', 'A', 30, 'Hall A 101', 'MONDAY 09:00-10:15, WEDNESDAY 09:00-10:15');
  perform pg_temp.add_section('MATH120', '2025FA', 'daniel.reyes@faculty.example', 'A', 30, 'Science 210', 'TUESDAY 09:00-10:15, THURSDAY 09:00-10:15');
  perform pg_temp.add_section('MATH140', '2025FA', 'daniel.reyes@faculty.example', 'A', 30, 'Science 210', 'MONDAY 11:00-11:50, WEDNESDAY 11:00-11:50, FRIDAY 11:00-11:50');
  perform pg_temp.add_section('ENG110', '2025FA', 'hannah.novak@faculty.example', 'A', 25, 'Library 3', 'TUESDAY 13:00-14:15, THURSDAY 13:00-14:15');
  perform pg_temp.add_section('CS150', '2025FA', 'meilin.chen@faculty.example', 'A', 30, 'Hall A 102', 'FRIDAY 13:00-15:30');

  -- Spring 2026
  perform pg_temp.add_section('CS201', '2026SP', 'meilin.chen@faculty.example', 'A', 25, 'Hall A 102', 'MONDAY 10:00-11:15, WEDNESDAY 10:00-11:15');
  perform pg_temp.add_section('CS220', '2026SP', 'amara.okafor@faculty.example', 'A', 25, 'Hall A 101', 'TUESDAY 10:00-11:15, THURSDAY 10:00-11:15');
  perform pg_temp.add_section('CS240', '2026SP', 'amara.okafor@faculty.example', 'A', 25, 'Lab 4', 'MONDAY 14:00-15:15, WEDNESDAY 14:00-15:15');
  perform pg_temp.add_section('PHYS110', '2026SP', 'samuel.adeyemi@faculty.example', 'A', 30, 'Physics 1', 'MONDAY 09:00-09:50, WEDNESDAY 09:00-09:50, FRIDAY 09:00-09:50');
  perform pg_temp.add_section('STAT200', '2026SP', 'priya.raman@faculty.example', 'A', 30, 'Science 112', 'TUESDAY 13:00-14:15, THURSDAY 13:00-14:15');
  perform pg_temp.add_section('MATH140', '2026SP', 'daniel.reyes@faculty.example', 'A', 30, 'Science 210', 'MONDAY 11:00-11:50, WEDNESDAY 11:00-11:50, FRIDAY 11:00-11:50');

  -- Fall 2026 (running): the same course taught by two professors (CS201-A and CS201-B)
  perform pg_temp.add_section('CS301', '2026FA', 'meilin.chen@faculty.example', 'A', 25, 'Hall A 102', 'MONDAY 10:00-11:15, WEDNESDAY 10:00-11:15');
  perform pg_temp.add_section('CS320', '2026FA', 'amara.okafor@faculty.example', 'A', 25, 'Hall A 101', 'TUESDAY 10:00-11:15, THURSDAY 10:00-11:15');
  perform pg_temp.add_section('CS340', '2026FA', 'amara.okafor@faculty.example', 'A', 25, 'Lab 4', 'MONDAY 13:00-14:15, WEDNESDAY 13:00-14:15');
  perform pg_temp.add_section('CS201', '2026FA', 'meilin.chen@faculty.example', 'A', 25, 'Hall A 102', 'TUESDAY 08:30-09:45, THURSDAY 08:30-09:45');
  perform pg_temp.add_section('CS201', '2026FA', 'priya.raman@faculty.example', 'B', 25, 'Science 112', 'MONDAY 15:30-16:45, WEDNESDAY 15:30-16:45');
  perform pg_temp.add_section('STAT310', '2026FA', 'priya.raman@faculty.example', 'A', 25, 'Science 112', 'TUESDAY 14:00-15:15, THURSDAY 14:00-15:15');
  perform pg_temp.add_section('ENG110', '2026FA', 'hannah.novak@faculty.example', 'A', 25, 'Library 3', 'TUESDAY 13:00-14:15, THURSDAY 13:00-14:15');
  perform pg_temp.add_section('PHYS110', '2026FA', 'samuel.adeyemi@faculty.example', 'A', 30, 'Physics 1', 'MONDAY 09:00-09:50, WEDNESDAY 09:00-09:50, FRIDAY 09:00-09:50');
  perform pg_temp.add_section('MATH140', '2026FA', 'daniel.reyes@faculty.example', 'A', 30, 'Science 210', 'MONDAY 11:00-11:50, WEDNESDAY 11:00-11:50, FRIDAY 11:00-11:50');

  -- Spring 2027 (enrollment open)
  perform pg_temp.add_section('CS401', '2027SP', 'amara.okafor@faculty.example', 'A', 20, 'Hall A 101', 'MONDAY 10:00-11:15, WEDNESDAY 10:00-11:15');
  perform pg_temp.add_section('CS320', '2027SP', 'amara.okafor@faculty.example', 'A', 20, 'Hall A 101', 'TUESDAY 13:00-14:15, THURSDAY 13:00-14:15');
  perform pg_temp.add_section('CS340', '2027SP', 'amara.okafor@faculty.example', 'A', 20, 'Lab 4', 'MONDAY 13:00-14:15, WEDNESDAY 13:00-14:15');
  perform pg_temp.add_section('CS340', '2027SP', 'priya.raman@faculty.example', 'B', 20, 'Lab 2', 'TUESDAY 08:00-09:15, THURSDAY 08:00-09:15');
  perform pg_temp.add_section('CS301', '2027SP', 'meilin.chen@faculty.example', 'A', 20, 'Hall A 102', 'TUESDAY 10:00-11:15, THURSDAY 10:00-11:15');
  perform pg_temp.add_section('CS101', '2027SP', 'meilin.chen@faculty.example', 'A', 30, 'Hall A 102', 'MONDAY 11:30-12:45, WEDNESDAY 11:30-12:45');
  perform pg_temp.add_section('CS250', '2027SP', 'meilin.chen@faculty.example', 'A', 20, 'Design Studio', 'TUESDAY 15:00-16:15, THURSDAY 15:00-16:15');
  perform pg_temp.add_section('CS220', '2027SP', 'meilin.chen@faculty.example', 'A', 20, 'Lab 1', 'MONDAY 13:30-14:45, WEDNESDAY 13:30-14:45');
  perform pg_temp.add_section('STAT310', '2027SP', 'priya.raman@faculty.example', 'A', 20, 'Science 112', 'MONDAY 10:00-11:15, WEDNESDAY 10:00-11:15');
  perform pg_temp.add_section('MATH120', '2027SP', 'daniel.reyes@faculty.example', 'A', 2, 'Seminar 5', 'TUESDAY 09:00-10:15, THURSDAY 09:00-10:15');
  perform pg_temp.add_section('MATH140', '2027SP', 'daniel.reyes@faculty.example', 'A', 30, 'Science 210', 'MONDAY 14:20-15:10, WEDNESDAY 14:20-15:10, FRIDAY 14:20-15:10');
  perform pg_temp.add_section('MATH140', '2027SP', 'daniel.reyes@faculty.example', 'B', 2, 'Seminar 5', 'TUESDAY 15:00-16:15, THURSDAY 15:00-16:15');
  perform pg_temp.add_section('PHYS110', '2027SP', 'samuel.adeyemi@faculty.example', 'A', 30, 'Physics 1', 'MONDAY 09:00-09:50, WEDNESDAY 09:00-09:50, FRIDAY 09:00-09:50');
  perform pg_temp.add_section('ENG110', '2027SP', 'hannah.novak@faculty.example', 'A', 25, 'Library 3', 'TUESDAY 13:00-14:15, THURSDAY 13:00-14:15');

  -- Fall 2027 (draft)
  perform pg_temp.add_section('CS101', '2027FA', 'amara.okafor@faculty.example', 'A', 30, 'Hall A 101', 'MONDAY 09:00-10:15, WEDNESDAY 09:00-10:15');
  perform pg_temp.add_section('CS201', '2027FA', 'meilin.chen@faculty.example', 'A', 25, 'Hall A 102', 'TUESDAY 10:00-11:15, THURSDAY 10:00-11:15');

  -- students (lena.fischer exists from R__demo_accounts)
  perform pg_temp.add_student('noah.kim@uni.example', 'Noah', 'Kim', 'ACTIVE', 2025, timestamptz '2025-07-10 10:00+00');
  perform pg_temp.add_student('sofia.martins@uni.example', 'Sofia', 'Martins', 'ACTIVE', 2025, timestamptz '2025-07-12 10:00+00');
  perform pg_temp.add_student('grace.okonkwo@uni.example', 'Grace', 'Okonkwo', 'ACTIVE', 2025, timestamptz '2025-07-02 10:00+00');
  perform pg_temp.add_student('zara.ahmed@uni.example', 'Zara', 'Ahmed', 'ACTIVE', 2025, timestamptz '2025-07-03 10:00+00');
  perform pg_temp.add_student('mia.rossi@uni.example', 'Mia', 'Rossi', 'ACTIVE', 2025, timestamptz '2025-07-20 10:00+00');
  perform pg_temp.add_student('liam.obrien@uni.example', 'Liam', 'O''Brien', 'INACTIVE', 2025, timestamptz '2025-07-21 10:00+00');
  perform pg_temp.add_student('ava.thompson@uni.example', 'Ava', 'Thompson', 'ACTIVE', 2026, timestamptz '2026-07-15 10:00+00');
  perform pg_temp.add_student('kenji.watanabe@uni.example', 'Kenji', 'Watanabe', 'ACTIVE', 2026, timestamptz '2026-07-16 10:00+00');
  perform pg_temp.add_student('omar.haddad@uni.example', 'Omar', 'Haddad', 'PENDING', 2026, timestamptz '2026-11-12 16:20+00');
  perform pg_temp.add_student('ethan.brooks@uni.example', 'Ethan', 'Brooks', 'REJECTED', 2026, timestamptz '2026-10-02 09:00+00');

  -- Lena: strong record; on Spring 2027 she meets every greyed-out reason
  perform pg_temp.enroll('lena.fischer@uni.example', 'CS101-A@2025FA', 'COMPLETED', 95);
  perform pg_temp.enroll('lena.fischer@uni.example', 'MATH120-A@2025FA', 'COMPLETED', 88);
  perform pg_temp.enroll('lena.fischer@uni.example', 'ENG110-A@2025FA', 'COMPLETED', 91);
  perform pg_temp.enroll('lena.fischer@uni.example', 'CS201-A@2026SP', 'COMPLETED', 94);
  perform pg_temp.enroll('lena.fischer@uni.example', 'CS240-A@2026SP', 'COMPLETED', 85);
  perform pg_temp.enroll('lena.fischer@uni.example', 'CS301-A@2026FA', 'ENROLLED');
  perform pg_temp.enroll('lena.fischer@uni.example', 'CS320-A@2026FA', 'ENROLLED', 89);
  perform pg_temp.enroll('lena.fischer@uni.example', 'CS340-A@2027SP', 'ENROLLED');

  -- Noah: 25 credits so far, 6 more in Fall 2026 (both graded already)
  perform pg_temp.enroll('noah.kim@uni.example', 'CS101-A@2025FA', 'COMPLETED', 96);
  perform pg_temp.enroll('noah.kim@uni.example', 'MATH120-A@2025FA', 'COMPLETED', 90);
  perform pg_temp.enroll('noah.kim@uni.example', 'MATH140-A@2025FA', 'COMPLETED', 85);
  perform pg_temp.enroll('noah.kim@uni.example', 'ENG110-A@2025FA', 'COMPLETED', 88);
  perform pg_temp.enroll('noah.kim@uni.example', 'CS201-A@2026SP', 'COMPLETED', 93);
  perform pg_temp.enroll('noah.kim@uni.example', 'CS220-A@2026SP', 'COMPLETED', 84);
  perform pg_temp.enroll('noah.kim@uni.example', 'CS240-A@2026SP', 'COMPLETED', 91);
  perform pg_temp.enroll('noah.kim@uni.example', 'STAT200-A@2026SP', 'COMPLETED', 87);
  perform pg_temp.enroll('noah.kim@uni.example', 'CS301-A@2026FA', 'ENROLLED', 92);
  perform pg_temp.enroll('noah.kim@uni.example', 'CS320-A@2026FA', 'ENROLLED', 88);

  -- Sofia: failed Calculus, passed it on the retake; a withdrawal; physics running now
  perform pg_temp.enroll('sofia.martins@uni.example', 'MATH140-A@2025FA', 'COMPLETED', 52);
  perform pg_temp.enroll('sofia.martins@uni.example', 'CS101-A@2025FA', 'COMPLETED', 84);
  perform pg_temp.enroll('sofia.martins@uni.example', 'ENG110-A@2025FA', 'COMPLETED', 78);
  perform pg_temp.enroll('sofia.martins@uni.example', 'MATH140-A@2026SP', 'COMPLETED', 75);
  perform pg_temp.enroll('sofia.martins@uni.example', 'CS240-A@2026SP', 'COMPLETED', 81);
  perform pg_temp.enroll('sofia.martins@uni.example', 'CS220-A@2026SP', 'WITHDRAWN');
  perform pg_temp.enroll('sofia.martins@uni.example', 'PHYS110-A@2026FA', 'ENROLLED', 71);

  -- Grace: graduated in May 2026 with 32 credits
  perform pg_temp.enroll('grace.okonkwo@uni.example', 'CS101-A@2025FA', 'COMPLETED', 97);
  perform pg_temp.enroll('grace.okonkwo@uni.example', 'MATH120-A@2025FA', 'COMPLETED', 93);
  perform pg_temp.enroll('grace.okonkwo@uni.example', 'MATH140-A@2025FA', 'COMPLETED', 90);
  perform pg_temp.enroll('grace.okonkwo@uni.example', 'ENG110-A@2025FA', 'COMPLETED', 94);
  perform pg_temp.enroll('grace.okonkwo@uni.example', 'CS150-A@2025FA', 'COMPLETED', 89);
  perform pg_temp.enroll('grace.okonkwo@uni.example', 'CS201-A@2026SP', 'COMPLETED', 95);
  perform pg_temp.enroll('grace.okonkwo@uni.example', 'CS220-A@2026SP', 'COMPLETED', 91);
  perform pg_temp.enroll('grace.okonkwo@uni.example', 'CS240-A@2026SP', 'COMPLETED', 96);
  perform pg_temp.enroll('grace.okonkwo@uni.example', 'STAT200-A@2026SP', 'COMPLETED', 88);
  perform pg_temp.enroll('grace.okonkwo@uni.example', 'PHYS110-A@2026SP', 'COMPLETED', 92);

  -- Zara: 32 credits, waiting for her graduation decision
  perform pg_temp.enroll('zara.ahmed@uni.example', 'CS101-A@2025FA', 'COMPLETED', 86);
  perform pg_temp.enroll('zara.ahmed@uni.example', 'MATH120-A@2025FA', 'COMPLETED', 79);
  perform pg_temp.enroll('zara.ahmed@uni.example', 'MATH140-A@2025FA', 'COMPLETED', 83);
  perform pg_temp.enroll('zara.ahmed@uni.example', 'ENG110-A@2025FA', 'COMPLETED', 90);
  perform pg_temp.enroll('zara.ahmed@uni.example', 'CS150-A@2025FA', 'COMPLETED', 72);
  perform pg_temp.enroll('zara.ahmed@uni.example', 'CS201-A@2026SP', 'COMPLETED', 81);
  perform pg_temp.enroll('zara.ahmed@uni.example', 'CS220-A@2026SP', 'COMPLETED', 77);
  perform pg_temp.enroll('zara.ahmed@uni.example', 'CS240-A@2026SP', 'COMPLETED', 88);
  perform pg_temp.enroll('zara.ahmed@uni.example', 'STAT200-A@2026SP', 'COMPLETED', 74);
  perform pg_temp.enroll('zara.ahmed@uni.example', 'PHYS110-A@2026SP', 'COMPLETED', 80);

  -- Mia, Liam, Ava, Kenji
  perform pg_temp.enroll('mia.rossi@uni.example', 'CS101-A@2025FA', 'COMPLETED', 91);
  perform pg_temp.enroll('mia.rossi@uni.example', 'ENG110-A@2025FA', 'COMPLETED', 85);
  perform pg_temp.enroll('mia.rossi@uni.example', 'MATH120-A@2025FA', 'COMPLETED', 74);
  perform pg_temp.enroll('mia.rossi@uni.example', 'CS240-A@2026SP', 'COMPLETED', 88);
  perform pg_temp.enroll('mia.rossi@uni.example', 'CS201-B@2026FA', 'ENROLLED', 86);
  perform pg_temp.enroll('liam.obrien@uni.example', 'CS101-A@2025FA', 'COMPLETED', 85);
  perform pg_temp.enroll('ava.thompson@uni.example', 'ENG110-A@2026FA', 'ENROLLED', 78);
  perform pg_temp.enroll('ava.thompson@uni.example', 'MATH140-A@2026FA', 'ENROLLED', 65);
  perform pg_temp.enroll('kenji.watanabe@uni.example', 'ENG110-A@2026FA', 'ENROLLED', 82);
  -- the two seats of MATH120-A and MATH140-B in Spring 2027 are taken
  perform pg_temp.enroll('ava.thompson@uni.example', 'MATH120-A@2027SP', 'ENROLLED');
  perform pg_temp.enroll('kenji.watanabe@uni.example', 'MATH120-A@2027SP', 'ENROLLED');
  perform pg_temp.enroll('mia.rossi@uni.example', 'MATH140-B@2027SP', 'ENROLLED');
  perform pg_temp.enroll('kenji.watanabe@uni.example', 'MATH140-B@2027SP', 'ENROLLED');

  -- graduation: Grace approved, Zara pending
  update students set academic_status = 'GRADUATED', certificate_no = 'KS-2026-0001', graduated_at = timestamptz '2026-05-20 15:00+00'
  where user_id = (select id from users where email = 'grace.okonkwo@uni.example');
  perform setval('certificate_no_seq', 1);
  insert into graduation_applications (student_id, status, applied_at, decided_at)
  select s.id, 'APPROVED', timestamptz '2026-05-12 10:00+00', timestamptz '2026-05-20 15:00+00'
  from students s join users u on u.id = s.user_id where u.email = 'grace.okonkwo@uni.example';
  insert into graduation_applications (student_id, status, applied_at)
  select s.id, 'PENDING', timestamptz '2026-11-02 11:30+00'
  from students s join users u on u.id = s.user_id where u.email = 'zara.ahmed@uni.example';

  -- notifications already in inboxes
  perform pg_temp.notify('admin@kaka.example', 'ALL_STUDENTS', null,
    'Spring 2027 enrollment is open',
    E'Enrollment for Spring 2027 is now open.\n\nChoose your sections from the Enroll page. Sections that are full, clash with your timetable or need a prerequisite are marked with the reason.',
    timestamptz '2026-11-09 09:00+00',
    array['lena.fischer@uni.example', 'noah.kim@uni.example', 'sofia.martins@uni.example', 'grace.okonkwo@uni.example', 'zara.ahmed@uni.example', 'mia.rossi@uni.example', 'ava.thompson@uni.example', 'kenji.watanabe@uni.example'],
    array['noah.kim@uni.example', 'grace.okonkwo@uni.example']);
  perform pg_temp.notify('admin@kaka.example', 'ALL_PROFESSORS', null,
    'Fall 2026 grades are due on December 11',
    E'Please enter every score before the semester ends on December 11. The semester cannot be closed while a student is ungraded.',
    timestamptz '2026-11-10 08:30+00',
    array['amara.okafor@faculty.example', 'daniel.reyes@faculty.example', 'meilin.chen@faculty.example', 'samuel.adeyemi@faculty.example', 'hannah.novak@faculty.example', 'priya.raman@faculty.example'],
    array['daniel.reyes@faculty.example']);
  perform pg_temp.notify('amara.okafor@faculty.example', 'SECTION', 'CS320-A@2026FA',
    'Project groups for CS320',
    E'Hello everyone,\n\nPlease form groups of two for the database project by Friday. Send me your partner''s name; anyone without a group will be paired up on Monday.\n\nAmara Okafor',
    timestamptz '2026-11-11 14:00+00',
    array['lena.fischer@uni.example', 'noah.kim@uni.example'],
    array[]::text[]);
  -- system notes: what the registrar told people, and two alerts waiting for the administrator
  perform pg_temp.system_note('lena.fischer@uni.example', 'Your account is approved',
    'Your registration was approved. Welcome to Kaka Student Management.', timestamptz '2026-07-02 09:00+00', true, true);
  perform pg_temp.system_note('grace.okonkwo@uni.example', 'Congratulations on your graduation',
    'Congratulations: your graduation was approved. Your certificate number is KS-2026-0001; print the certificate from the Graduation page.',
    timestamptz '2026-05-20 15:00+00', true, false);
  perform pg_temp.system_note('amara.okafor@faculty.example', 'You teach CS401-A Distributed Systems in Spring 2027',
    'You teach CS401-A Distributed Systems in Spring 2027: Mon 10:00–11:15, Wed 10:00–11:15 in Hall A 101.',
    timestamptz '2026-10-20 10:00+00', true, false);
  perform pg_temp.system_note('admin@kaka.example', 'Graduation application: Zara Ahmed',
    'Zara Ahmed applied to graduate with 32 earned credits. Decide under Graduation.', timestamptz '2026-11-02 11:30+00', false, false);
  perform pg_temp.system_note('admin@kaka.example', 'New registration: Omar Haddad',
    'Omar Haddad (omar.haddad@uni.example) registered and is waiting for approval. Approve or reject it under Students.',
    timestamptz '2026-11-12 16:20+00', false, false);
end $$;

-- One student's transcript, the way the Transcript page builds it.
-- Dropped sections leave no trace; withdrawals show as W and do not count in GPA.
select sem.name                                as semester,
       c.code || '-' || s.letter               as section,
       c.title,
       s.credits,
       e.status,
       e.score,
       case when e.status = 'WITHDRAWN' then 'W' else e.letter_grade end as grade,
       e.grade_points
from enrollments e
join students st   on st.id = e.student_id
join users u       on u.id = st.user_id
join sections s    on s.id = e.section_id
join courses c     on c.id = s.course_id
join semesters sem on sem.id = s.semester_id
where u.email = 'sofia.martins@uni.example'
  and e.status <> 'DROPPED'
order by sem.start_date, c.code;

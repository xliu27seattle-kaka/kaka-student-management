-- Cumulative GPA and earned credits per student, computed in SQL.
-- It should match what the app shows: F counts as 0 points, W is left out,
-- only completed (ended-semester) attempts count, and earned credits count each passed course once.
with graded as (
  select e.student_id, s.credits, e.grade_points, e.score, s.course_id
  from enrollments e join sections s on s.id = e.section_id
  where e.status = 'COMPLETED' and e.grade_points is not null
),
earned as (
  select student_id, sum(credits) as credits
  from (select distinct on (student_id, course_id) student_id, course_id, credits
        from graded where score >= 60) passed
  group by student_id
)
select st.student_no,
       u.first_name || ' ' || u.last_name                                   as student,
       round(sum(g.grade_points * g.credits) / nullif(sum(g.credits), 0), 2) as gpa,
       coalesce(max(ea.credits), 0)                                         as earned_credits
from students st
join users u on u.id = st.user_id
left join graded g  on g.student_id = st.id
left join earned ea on ea.student_id = st.id
group by st.id, st.student_no, u.first_name, u.last_name
order by gpa desc nulls last;

-- Every section of a semester with its professor, schedule and seats taken.
-- Change the semester code to look at another one.
select c.code || '-' || s.letter                      as section,
       c.title,
       u.first_name || ' ' || u.last_name             as professor,
       string_agg(left(m.day_of_week, 3) || ' ' || to_char(m.start_time, 'HH24:MI') || '-' ||
                  to_char(m.end_time, 'HH24:MI'), ', ' order by m.position) as schedule,
       (select count(*) from enrollments e where e.section_id = s.id and e.status = 'ENROLLED') as enrolled,
       s.capacity
from sections s
join semesters sem   on sem.id = s.semester_id
join courses c       on c.id = s.course_id
join professors p    on p.id = s.professor_id
join users u         on u.id = p.user_id
left join section_meetings m on m.section_id = s.id
where sem.code = '2027SP'
group by s.id, c.code, c.title, u.first_name, u.last_name
order by section;

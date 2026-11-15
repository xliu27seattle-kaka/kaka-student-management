-- The prerequisite graph, and every chain of requirements leading to a course
-- (a recursive query: CS401 needs CS301 and CS320, which need CS201, which needs CS101 and MATH120).
select c.code as course, p.code as requires
from course_prerequisites cp
join courses c on c.id = cp.course_id
join courses p on p.id = cp.prerequisite_id
order by c.code, p.code;

with recursive chain(course_id, path, depth) as (
  select id, code::text, 0 from courses where code = 'CS401'
  union all
  select cp.prerequisite_id, chain.path || ' <- ' || p.code, chain.depth + 1
  from chain
  join course_prerequisites cp on cp.course_id = chain.course_id
  join courses p on p.id = cp.prerequisite_id
)
select path, depth from chain where depth > 0 order by depth, path;

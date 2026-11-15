-- How the emails are doing: the transactional outbox, newest first.
-- PENDING rows are waiting for a send or a retry; FAILED rows were given up on.
select status, count(*) from email_outbox group by status;

select id, recipient, subject, status, attempts, last_error, created_at, sent_at
from email_outbox
order by id desc
limit 20;

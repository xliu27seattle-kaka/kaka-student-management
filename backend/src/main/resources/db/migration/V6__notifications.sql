-- Notifications: one row per message, one recipient row per person who gets it.

create table notifications (
  id              bigserial primary key,
  sender_id       bigint       not null references users (id),
  audience        varchar(20)  not null,             -- ALL_STUDENTS | ALL_PROFESSORS | EVERYONE | SECTION
  section_id      bigint       references sections (id),
  subject         varchar(150) not null,
  body            text         not null,
  recipient_count int          not null default 0,
  created_at      timestamptz  not null default now()
);

create table notification_recipients (
  id              bigserial primary key,
  notification_id bigint      not null references notifications (id) on delete cascade,
  user_id         bigint      not null references users (id) on delete cascade,
  read_at         timestamptz,
  email_outbox_id bigint      references email_outbox (id) on delete set null,
  unique (notification_id, user_id)
);

create index notification_recipients_inbox on notification_recipients (user_id, read_at);

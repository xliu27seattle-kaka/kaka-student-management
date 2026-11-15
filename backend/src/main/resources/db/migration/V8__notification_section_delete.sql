-- A notification outlives the section it was sent to: deleting a draft section keeps the
-- message in everyone's inbox, without the section link.

alter table notifications drop constraint notifications_section_id_fkey;
alter table notifications
  add constraint notifications_section_id_fkey
  foreign key (section_id) references sections (id) on delete set null;

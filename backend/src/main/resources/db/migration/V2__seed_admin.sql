-- The one built-in administrator. Demo password: KakaDemo2026 (BCrypt below).
insert into users (email, password_hash, first_name, last_name, role, status)
values ('admin@kaka.example', '$2y$10$xbk5lVSHxSkdG5SmcJTjhuVM/bHaTTH0otH7e8tXVtddm0FlrDSim', 'Kaka', 'Admin', 'ADMIN', 'ACTIVE');

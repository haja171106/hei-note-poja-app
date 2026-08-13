create table if not exists course (
    id uuid primary key default gen_random_uuid(),
    ref varchar(50) not null unique,
    title varchar(200) not null,
    credit integer not null,
    semester_number integer not null,
    track varchar(10)
);

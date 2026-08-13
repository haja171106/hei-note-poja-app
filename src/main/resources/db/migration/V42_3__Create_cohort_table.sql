create table if not exists cohort (
    id uuid primary key default gen_random_uuid(),
    ref varchar(10) not null unique,
    entry_year integer not null
);

create table if not exists "user" (
    id uuid primary key default gen_random_uuid(),
    ref varchar(50) not null unique,
    name varchar(100) not null,
    firstname varchar(100) not null,
    email varchar(255) not null unique,
    password varchar(255),
    role varchar(20) not null,
    track varchar(10),
    cursus_status varchar(20),
    cohort_id uuid constraint fk_user_cohort references cohort(id),
    birthdate date,
    address varchar(255)
);

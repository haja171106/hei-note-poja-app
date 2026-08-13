create table if not exists "group" (
    id uuid primary key default gen_random_uuid(),
    ref varchar(20) not null,
    cohort_id uuid not null constraint fk_group_cohort references cohort(id)
);

create table if not exists student_group_history (
    id uuid primary key default gen_random_uuid(),
    student_id uuid not null constraint fk_sgh_student references "user"(id),
    group_id uuid not null constraint fk_sgh_group references "group"(id),
    start_date date not null,
    end_date date
);

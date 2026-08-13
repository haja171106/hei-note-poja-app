create table if not exists grade_history (
    id uuid primary key default gen_random_uuid(),
    grade_id uuid not null constraint fk_gh_grade references grade(id),
    old_value double precision,
    new_value double precision not null,
    changed_by uuid constraint fk_gh_changed_by references "user"(id),
    changed_at timestamp with time zone not null,
    reason varchar(500) not null
);

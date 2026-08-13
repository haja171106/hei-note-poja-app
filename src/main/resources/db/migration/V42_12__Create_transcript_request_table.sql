create table if not exists transcript_request (
    id uuid primary key default gen_random_uuid(),
    student_id uuid not null constraint fk_tr_student references "user"(id),
    academic_year integer not null,
    status varchar(20) not null,
    requested_at timestamp with time zone not null,
    requested_by uuid constraint fk_tr_requested_by references "user"(id)
);

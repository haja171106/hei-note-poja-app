create table if not exists grade (
    id uuid primary key default gen_random_uuid(),
    exam_id uuid not null constraint fk_grade_exam references exam(id),
    student_id uuid not null constraint fk_grade_student references "user"(id),
    value double precision not null,
    entered_by uuid constraint fk_grade_entered_by references "user"(id),
    entered_at timestamp with time zone not null,
    constraint uq_grade_exam_student unique (exam_id, student_id)
);

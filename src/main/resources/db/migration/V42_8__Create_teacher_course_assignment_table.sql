create table if not exists teacher_course_assignment (
    id uuid primary key default gen_random_uuid(),
    teacher_id uuid not null constraint fk_tca_teacher references "user"(id),
    course_id uuid not null constraint fk_tca_course references course(id),
    academic_year integer not null,
    constraint uq_teacher_course_year unique (teacher_id, course_id, academic_year)
);

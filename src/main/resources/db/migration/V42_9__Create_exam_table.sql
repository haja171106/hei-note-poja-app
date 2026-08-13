create table if not exists exam (
    id uuid primary key default gen_random_uuid(),
    course_id uuid not null constraint fk_exam_course references course(id),
    academic_year integer not null,
    label varchar(100) not null,
    date_exam timestamp with time zone not null,
    coefficient double precision not null
);

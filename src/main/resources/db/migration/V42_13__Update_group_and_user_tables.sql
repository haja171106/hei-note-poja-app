alter table "group" add column if not exists academic_year integer;
alter table "user" add column if not exists password varchar(255);

alter table if exists fallout add column municipality_id varchar(255) AFTER id;
alter table if exists history add column municipality_id varchar(255) AFTER id;
alter table if exists scheduled_job_log add column municipality_id varchar(255) AFTER id;
   
create index idx_municipality_id on fallout (municipality_id);
create index idx_municipality_id on history (municipality_id);
create index idx_municipality_id on scheduled_job_log (municipality_id);

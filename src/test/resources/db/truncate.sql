select 'Truncating fallout, history, scheduled_job_log, and shedlock tables' as 'Truncating fallout, history, scheduled_job_log, and shedlock tables';

truncate table fallout;
truncate table history;
truncate table scheduled_job_log;
truncate table shedlock;

select * from fallout;
select * from history;
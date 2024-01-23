-- drop idx_imaged_moments__elapsed_time
drop index if exists idx_imaged_moments__elapsed_time on imaged_moments
go

alter table imaged_moments
    alter column elapsed_time_millis bigint null
go

-- add idx_imaged_moments__elapsed_time
create index idx_imaged_moments__elapsed_time on imaged_moments (elapsed_time_millis)
go

drop index if exists idx_imaged_moments__recorded_timestamp on imaged_moments
go

alter table dbo.imaged_moments
    alter column recorded_timestamp DATETIMEOFFSET(6) null
go

create index idx_imaged_moments__recorded_timestamp on imaged_moments (recorded_timestamp)
go

alter table dbo.observations
    alter column duration_millis bigint null
go

alter table dbo.observations
    alter column observation_timestamp DATETIMEOFFSET(6) null
go
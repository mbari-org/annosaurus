-- drop idx_imaged_moments__elapsed_time
drop index if exists idx_imaged_moments__elapsed_time on imaged_moments
go
alter table
    imaged_moments
    alter column
        elapsed_time_millis bigint null
go
-- add idx_imaged_moments__elapsed_time
create index idx_imaged_moments__elapsed_time on imaged_moments (elapsed_time_millis)
go
drop index if exists idx_imaged_moments__recorded_timestamp on imaged_moments
go
alter table
    dbo.imaged_moments
    alter column
        recorded_timestamp DATETIMEOFFSET(6) null
go
update
    dbo.imaged_moments
set recorded_timestamp = convert(datetime2, recorded_timestamp) AT TIME ZONE 'UTC'
go
create index idx_imaged_moments__recorded_timestamp on imaged_moments (recorded_timestamp)
go
alter table
    dbo.observations
    alter column
        duration_millis bigint null
go
alter table
    dbo.observations
    alter column
        observation_timestamp DATETIMEOFFSET(6) null
go
update
    dbo.observations
set observation_timestamp = convert(datetime2, observation_timestamp) AT TIME ZONE 'UTC'
go
-- Add audit tables
create table associations_AUD
(
    REV              int              not null,
    REVTYPE          smallint,
    observation_uuid uniqueidentifier,
    uuid             uniqueidentifier not null,
    mime_type        varchar(64),
    link_name        varchar(128),
    to_concept       varchar(128),
    link_value       varchar(1024),
    primary key (REV, uuid)
)
GO
create table observations_AUD
(
    REV                   int              not null,
    REVTYPE               smallint,
    duration_millis       bigint,
    observation_timestamp datetimeoffset(6),
    imaged_moment_uuid    uniqueidentifier,
    uuid                  uniqueidentifier not null,
    activity              varchar(128),
    observation_group     varchar(128),
    observer              varchar(128),
    concept               varchar(256),
    primary key (REV, uuid)
)
GO
create table REVINFO
(
    REV      int identity not null,
    REVTSTMP bigint,
    primary key (REV)
)
GO
alter table
    associations_AUD
    add
        constraint fk_associations_aud__revinfo foreign key (REV) references REVINFO
GO
alter table
    observations_AUD
    add
        constraint fk_observations_aud_refinfo foreign key (REV) references REVINFO
GO
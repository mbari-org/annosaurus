create table ancillary_data
(
    altitude                    float(52),
    depth_meters                float(52),
    latitude                    float(52),
    light_transmission          float(52),
    longitude                   float(52),
    oxygen_ml_per_l             float(52),
    phi                         float(52),
    pressure_dbar               float(52),
    psi                         float(52),
    salinity                    float(52),
    temperature_celsius         float(52),
    theta                       float(52),
    x                           float(52),
    y                           float(52),
    z                           float(52),
    last_updated_time      timestamp,
    uuid                        char(36) not null,
    coordinate_reference_system varchar(32),
    imaged_moment_uuid          char(36) not null unique,
    xyz_position_units          varchar(255),
    primary key (uuid)
);
create table associations
(
    last_updated_time timestamp,
    observation_uuid       char(36) not null,
    uuid                   char(36) not null,
    mime_type              varchar(64)  not null,
    link_name              varchar(128) not null,
    to_concept             varchar(128),
    link_value             varchar(1024),
    primary key (uuid)
);
create table associations_AUD
(
    REV              integer not null,
    REVTYPE          smallint,
    observation_uuid char(36),
    uuid             char(36) not null,
    mime_type        varchar(64),
    link_name        varchar(128),
    to_concept       varchar(128),
    link_value       varchar(1024),
    primary key (REV, uuid)
);
create table image_references
(
    height_pixels          integer,
    width_pixels           integer,
    last_updated_time timestamp,
    imaged_moment_uuid     char(36) not null,
    uuid                   char(36) not null,
    format                 varchar(64),
    description            varchar(256),
    url                    varchar(1024) not null unique,
    primary key (uuid)
);
create table image_references_AUD
(
    REV                integer not null,
    REVTYPE            smallint,
    height_pixels      integer,
    width_pixels       integer,
    imaged_moment_uuid char(36),
    uuid               char(36) not null,
    format             varchar(64),
    description        varchar(256),
    url                varchar(1024),
    primary key (REV, uuid)
);
create table imaged_moments
(
    elapsed_time_millis    bigint,
    last_updated_time timestamp,
    recorded_timestamp     timestamp,
    uuid                   char(36) not null,
    timecode               varchar(255),
    video_reference_uuid   varchar(36),
    primary key (uuid)
);
create table imaged_moments_AUD
(
    REV                  integer not null,
    REVTYPE              smallint,
    elapsed_time_millis  bigint,
    recorded_timestamp   timestamp,
    uuid                 char(36) not null,
    timecode             varchar(255),
    video_reference_uuid char(36),
    primary key (REV, uuid)
);
create table observations
(
    duration_millis        bigint,
    last_updated_time timestamp,
    observation_timestamp  timestamp not null,
    imaged_moment_uuid     char(36) not null,
    uuid                   char(36) not null,
    activity               varchar(128),
    observation_group      varchar(128),
    observer               varchar(128),
    concept                varchar(256),
    primary key (uuid)
);
create table observations_AUD
(
    REV                   integer not null,
    REVTYPE               smallint,
    duration_millis       bigint,
    observation_timestamp timestamp,
    imaged_moment_uuid    char(36),
    uuid                  char(36) not null,
    activity              varchar(128),
    observation_group     varchar(128),
    observer              varchar(128),
    concept               varchar(256),
    primary key (REV, uuid)
);
create table REVINFO
(
    REV      integer generated by default as identity,
    REVTSTMP bigint,
    primary key (REV)
);
create table video_reference_information
(
    last_updated_time timestamp,
    uuid                   char(36) not null,
    mission_contact        varchar(64),
    platform_name          varchar(64)  not null,
    mission_id             varchar(256) not null,
    video_reference_uuid   char(36) unique,
    primary key (uuid)
);
create index idx_ancillary_data__imaged_moment_uuid on ancillary_data (imaged_moment_uuid);
create index idx_ancillary_data__position on ancillary_data (latitude, longitude, depth_meters);
create index idx_associations__link_name on associations (link_name);
create index idx_associations__link_value on associations (link_value);
create index idx_associations__to_concept on associations (to_concept);
create index idx_associations__observation_uuid on associations (observation_uuid);
create index idx_image_references__url on image_references (url);
create index idx_image_references__imaged_moment_uuid on image_references (imaged_moment_uuid);
create index idx_imaged_moments__video_reference_uuid on imaged_moments (video_reference_uuid);
create index idx_imaged_moments__recorded_timestamp on imaged_moments (recorded_timestamp);
create index idx_imaged_moments__elapsed_time on imaged_moments (elapsed_time_millis);
create index idx_imaged_moments__timecode on imaged_moments (timecode);
create index idx_observations__concept on observations (concept);
create index idx_observations__group on observations (observation_group);
create index idx_observations__activity on observations (activity);
create index idx_observations__imaged_moment_uuid on observations (imaged_moment_uuid);
create index idx_video_reference_information__video_reference_uuid on video_reference_information (video_reference_uuid);
alter table ancillary_data
    add constraint fk_ancillary_data__imaged_moment_uuid foreign key (imaged_moment_uuid) references imaged_moments;
alter table associations
    add constraint fk_associations__observations_uuid foreign key (observation_uuid) references observations;
alter table associations_AUD
    add constraint fk_associations__rev foreign key (REV) references REVINFO;
alter table image_references
    add constraint fk_image_references__imaged_moment_uuid foreign key (imaged_moment_uuid) references imaged_moments;
alter table image_references_AUD
    add constraint fk_image_references__rev foreign key (REV) references REVINFO;
alter table imaged_moments_AUD
    add constraint fk_imaged_moments__rev foreign key (REV) references REVINFO;
alter table observations
    add constraint fk_observations__imaged_moment_uuid foreign key (imaged_moment_uuid) references imaged_moments;
alter table observations_AUD
    add constraint fk_observations__rev foreign key (REV) references REVINFO;

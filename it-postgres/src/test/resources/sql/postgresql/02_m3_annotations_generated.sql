create table ancillary_data
(
    altitude                    float4,
    depth_meters                float4,
    latitude                    float(53),
    light_transmission          float4,
    longitude                   float(53),
    oxygen_ml_per_l             float4,
    phi                         float(53),
    pressure_dbar               float4,
    psi                         float(53),
    salinity                    float4,
    temperature_celsius         float4,
    theta                       float(53),
    x                           float(53),
    y                           float(53),
    z                           float(53),
    last_updated_time           timestamp(6),
    imaged_moment_uuid          uuid not null unique,
    uuid                        uuid not null,
    coordinate_reference_system varchar(32),
    xyz_position_units          varchar(255),
    primary key (uuid)
);
create table associations
(
    last_updated_time timestamp(6),
    observation_uuid  uuid         not null,
    uuid              uuid         not null,
    mime_type         varchar(64)  not null,
    link_name         varchar(128) not null,
    to_concept        varchar(128),
    link_value        varchar(1024),
    primary key (uuid)
);
create table image_references
(
    height_pixels      integer,
    width_pixels       integer,
    last_updated_time  timestamp(6),
    imaged_moment_uuid uuid          not null,
    uuid               uuid          not null,
    format             varchar(64),
    description        varchar(256),
    url                varchar(1024) not null unique,
    primary key (uuid)
);
create table imaged_moments
(
    elapsed_time_millis  bigint,
    last_updated_time    timestamp(6),
    recorded_timestamp   timestamp(6) with time zone,
    uuid                 uuid not null,
    video_reference_uuid uuid not null,
    timecode             varchar(255),
    primary key (uuid)
);
create table observations
(
    duration_millis       bigint,
    last_updated_time     timestamp(6),
    observation_timestamp timestamp(6) with time zone not null,
    imaged_moment_uuid    uuid                        not null,
    uuid                  uuid                        not null,
    activity              varchar(128),
    observation_group     varchar(128),
    observer              varchar(128),
    concept               varchar(256),
    primary key (uuid)
);
create table video_reference_information
(
    last_updated_time    timestamp(6),
    uuid                 uuid         not null,
    video_reference_uuid uuid         not null unique,
    mission_contact      varchar(64),
    platform_name        varchar(64)  not null,
    mission_id           varchar(256) not null,
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
alter table if exists ancillary_data add constraint fk_ancillary_data__imaged_moment_uuid foreign key (imaged_moment_uuid) references imaged_moments;
alter table if exists associations add constraint fk_associations__observation_uuid foreign key (observation_uuid) references observations;
alter table if exists image_references add constraint fk_image_references__imaged_moment_uuid foreign key (imaged_moment_uuid) references imaged_moments;
alter table if exists observations add constraint fk_observations__imaged_moment_uuid foreign key (imaged_moment_uuid) references imaged_moments;

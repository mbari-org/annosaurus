create table ancillary_data
(
    altitude                    float(24),
    depth_meters                float(24),
    latitude                    float(53),
    light_transmission          float(24),
    longitude                   float(53),
    oxygen_ml_per_l             float(24),
    phi                         float(53),
    pressure_dbar               float(24),
    psi                         float(53),
    salinity                    float(24),
    temperature_celsius         float(24),
    theta                       float(53),
    x                           float(53),
    y                           float(53),
    z                           float(53),
    last_updated_timestamp           datetime2(6),
    imaged_moment_uuid          uniqueidentifier not null,
    uuid                        uniqueidentifier not null,
    coordinate_reference_system varchar(32),
    xyz_position_units          varchar(255),
    primary key (uuid)
);
create table associations
(
    last_updated_timestamp datetime2(6),
    observation_uuid  uniqueidentifier not null,
    uuid              uniqueidentifier not null,
    mime_type         varchar(64)      not null,
    link_name         varchar(128)     not null,
    to_concept        varchar(128),
    link_value        varchar(1024),
    primary key (uuid)
);
create table image_references
(
    height_pixels      int,
    width_pixels       int,
    last_updated_timestamp  datetime2(6),
    imaged_moment_uuid uniqueidentifier not null,
    uuid               uniqueidentifier not null,
    format             varchar(64),
    description        varchar(256),
    url                varchar(2048)    not null,
    primary key (uuid)
);
create table imaged_moments
(
    elapsed_time_millis  bigint,
    last_updated_timestamp    datetime2(6),
    recorded_timestamp   datetimeoffset(6),
    uuid                 uniqueidentifier not null,
    video_reference_uuid uniqueidentifier not null,
    timecode             varchar(11),
    primary key (uuid)
);
create table observations
(
    duration_millis       bigint,
    last_updated_timestamp     datetime2(6),
    observation_timestamp datetimeoffset(6) not null,
    imaged_moment_uuid    uniqueidentifier  not null,
    uuid                  uniqueidentifier  not null,
    activity              varchar(128),
    observation_group     varchar(128),
    observer              varchar(128),
    concept               varchar(256),
    primary key (uuid)
);
create table video_reference_information
(
    last_updated_timestamp    datetime2(6),
    uuid                 uniqueidentifier not null,
    video_reference_uuid uniqueidentifier not null,
    mission_contact      varchar(64),
    platform_name        varchar(64)      not null,
    mission_id           varchar(256)     not null,
    primary key (uuid)
);
create index idx_ancillary_data__imaged_moment_uuid on ancillary_data (imaged_moment_uuid);
create index idx_ancillary_data__position on ancillary_data (latitude, longitude, depth_meters);
alter table ancillary_data
    add constraint uk_ancillary_data__imaged_moment_uuid unique (imaged_moment_uuid);
create index idx_associations__link_name on associations (link_name);
create index idx_associations__link_value on associations (link_value);
create index idx_associations__to_concept on associations (to_concept);
create index idx_associations__observation_uuid on associations (observation_uuid);
create index idx_image_references__url on image_references (url);
create index idx_image_references__imaged_moment_uuid on image_references (imaged_moment_uuid);
alter table image_references
    add constraint uk_image_references__url unique (url);
create index idx_imaged_moments__video_reference_uuid on imaged_moments (video_reference_uuid);
create index idx_imaged_moments__recorded_timestamp on imaged_moments (recorded_timestamp);
create index idx_imaged_moments__elapsed_time on imaged_moments (elapsed_time_millis);
create index idx_imaged_moments__timecode on imaged_moments (timecode);
create index idx_observations__concept on observations (concept);
create index idx_observations__group on observations (observation_group);
create index idx_observations__activity on observations (activity);
create index idx_observations__imaged_moment_uuid on observations (imaged_moment_uuid);
create index idx_video_reference_information__video_reference_uuid on video_reference_information (video_reference_uuid);
alter table video_reference_information
    add constraint uk_video_reference_information__video_reference_uuid unique (video_reference_uuid);
alter table ancillary_data
    add constraint fk_ancillary_data__imaged_moment_uuid foreign key (imaged_moment_uuid) references imaged_moments;
alter table associations
    add constraint fk_associations__observation_uuid foreign key (observation_uuid) references observations;
alter table image_references
    add constraint fk_image_references__imaged_moment_uuid foreign key (imaged_moment_uuid) references imaged_moments;
alter table observations
    add constraint fk_observations__imaged_moment_uuid foreign key (imaged_moment_uuid) references imaged_moments;

CREATE TABLE "dbo"."adjust_file_histories"  (
	"id"                  	bigint IDENTITY(100,1) NOT NULL,
	"uuid"                	uniqueidentifier NOT NULL,
	"video_reference_uuid"	uniqueidentifier NOT NULL,
	"adjust_timestamp"    	datetime2 NOT NULL,
	CONSTRAINT "PK__adjust_f__3213E83FD0AAC86F" PRIMARY KEY CLUSTERED("id")
 ON [PRIMARY]);

CREATE TABLE "dbo"."adjust_rov_tape_histories"  ( 
	"id"                  	bigint IDENTITY(100,1) NOT NULL,
	"uuid"                	uniqueidentifier NOT NULL,
	"video_reference_uuid"	uniqueidentifier NOT NULL,
	"media_type"          	nvarchar(16) NOT NULL,
	"adjust_type"         	nvarchar(16) NOT NULL,
	"status_message"      	nvarchar(512) NULL,
	"observation_count"   	int NOT NULL,
	"adjust_timestamp"    	datetime2 NOT NULL,
	CONSTRAINT "PK__adjust_r__3213E83F21252C06" PRIMARY KEY CLUSTERED("id")
 ON [PRIMARY]);

 CREATE TABLE "dbo"."merge_rov_histories"  ( 
	"id"                  	bigint IDENTITY(1,1) NOT NULL,
	"uuid"                	uniqueidentifier NOT NULL,
	"video_reference_uuid"	uniqueidentifier NOT NULL,
	"merge_timestamp"     	datetime2 NOT NULL,
	"media_type"          	nvarchar(16) NOT NULL,
	"status_message"      	nvarchar(512) NULL,
	"is_navigation_edited"	smallint NOT NULL,
	"imaged_moment_count" 	int NOT NULL,
	CONSTRAINT "PK_merge_rov_histories" PRIMARY KEY CLUSTERED("id")
 ON [PRIMARY]);

CREATE VIEW "annotations"
AS
SELECT
    im.uuid AS imaged_moment_uuid,
    im.elapsed_time_millis AS index_elapsed_time_millis,
    im.recorded_timestamp AS index_recorded_timestamp,
    im.timecode AS index_timecode,
    obs.uuid AS observation_uuid,
    obs.activity,
    obs.concept,
    obs.duration_millis,
    obs.observation_group,
    obs.observation_timestamp,
    obs.observer,
    ir.uuid AS image_reference_uuid,
    ir.description AS image_description,
    ir.format AS image_format,
    ir.height_pixels AS image_height,
    ir.width_pixels AS image_width,
    ir.url AS image_url,
    ass.link_name,
    ass.link_value,
    ass.to_concept,
    ass.mime_type AS association_mime_type,
    CONCAT(ass.link_name, ' | ', ass.to_concept, ' | ', ass.link_value) AS associations,
    ad.altitude,
    ad.coordinate_reference_system,
    ad.depth_meters,
    ad.latitude,
    ad.longitude,
    ad.oxygen_ml_per_l,
    ad.phi,
    ad.xyz_position_units,
    ad.pressure_dbar,
    ad.psi,
    ad.salinity,
    ad.temperature_celsius,
    ad.theta,
    ad.x,
    ad.y,
    ad.z,
    ad.light_transmission,
    info.mission_contact AS chief_scientist,
    info.mission_id AS dive_number,
    info.platform_name AS camera_platform
FROM
    imaged_moments im
        LEFT JOIN observations obs ON obs.imaged_moment_uuid = im.uuid
        LEFT JOIN image_references ir ON ir.imaged_moment_uuid = im.uuid
        LEFT JOIN associations ass ON ass.observation_uuid = obs.uuid
        LEFT JOIN ancillary_data  ad ON ad.imaged_moment_uuid = im.uuid
        LEFT JOIN video_reference_information info ON info.video_reference_uuid = im.video_reference_uuid;


--  ALTER TABLE "dbo"."ancillary_data" WITH NOCHECK
-- 	ADD CONSTRAINT "FK_ancillary_data_imaged_moment_uuid"
-- 	FOREIGN KEY("imaged_moment_uuid")
-- 	REFERENCES "dbo"."imaged_moments"("uuid")
-- 	ON DELETE NO ACTION
-- 	ON UPDATE NO ACTION;
--
-- ALTER TABLE "dbo"."associations"
-- 	ADD CONSTRAINT "FK_associations_observation_uuid"
-- 	FOREIGN KEY("observation_uuid")
-- 	REFERENCES "dbo"."observations"("uuid")
-- 	ON DELETE NO ACTION
-- 	ON UPDATE NO ACTION;
--
-- ALTER TABLE "dbo"."image_references" WITH NOCHECK
-- 	ADD CONSTRAINT "FK_image_references_imaged_moment_uuid"
-- 	FOREIGN KEY("imaged_moment_uuid")
-- 	REFERENCES "dbo"."imaged_moments"("uuid")
-- 	ON DELETE NO ACTION
-- 	ON UPDATE NO ACTION;
--
-- ALTER TABLE "dbo"."observations"
-- 	ADD CONSTRAINT "FK_observations_imaged_moment_uuid"
-- 	FOREIGN KEY("imaged_moment_uuid")
-- 	REFERENCES "dbo"."imaged_moments"("uuid")
-- 	ON DELETE NO ACTION
-- 	ON UPDATE NO ACTION;

create table associations_AUD (REV int not null, REVTYPE smallint, observation_uuid uniqueidentifier, uuid uniqueidentifier not null, mime_type varchar(64), link_name varchar(128), to_concept varchar(128), link_value varchar(1024), primary key (REV, uuid));
create table observations_AUD (REV int not null, REVTYPE smallint, duration_millis bigint, observation_timestamp datetimeoffset(6), imaged_moment_uuid uniqueidentifier, uuid uniqueidentifier not null, activity varchar(128), observation_group varchar(128), observer varchar(128), concept varchar(256), primary key (REV, uuid));
create table REVINFO (REV int identity not null, REVTSTMP bigint, primary key (REV));
alter table associations_AUD add constraint fk_associations_aud__revinfo foreign key (REV) references REVINFO;
alter table observations_AUD add constraint fk_observations_aud_refinfo foreign key (REV) references REVINFO;
create table ancillary_data
(
    altitude                    float(53),
    depth_meters                float(53),
    latitude                    float(53),
    light_transmission          float(53),
    longitude                   float(53),
    oxygen_ml_per_l             float(53),
    phi                         float(53),
    pressure_dbar               float(53),
    psi                         float(53),
    salinity                    float(53),
    temperature_celsius         float(53),
    theta                       float(53),
    x                           float(53),
    y                           float(53),
    z                           float(53),
    last_updated_time           datetime2(6),
    imaged_moment_uuid          uniqueidentifier not null,
    uuid                        uniqueidentifier not null,
    coordinate_reference_system varchar(32),
    xyz_position_units          varchar(32),
    primary key (uuid)
);
create table associations
(
    last_updated_time datetime2(6),
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
    last_updated_time  datetime2(6),
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
    last_updated_time    datetime2(6),
    recorded_timestamp   datetimeoffset(6),
    uuid                 uniqueidentifier not null,
    video_reference_uuid uniqueidentifier not null,
    timecode             varchar(255),
    primary key (uuid)
);
create table observations
(
    duration_millis       bigint,
    last_updated_time     datetime2(6),
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
    last_updated_time    datetime2(6),
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
    add constraint UK_2yx268s7fsveo44c4eaydm0b3 unique (imaged_moment_uuid);
create index idx_associations__link_name on associations (link_name);
create index idx_associations__link_value on associations (link_value);
create index idx_associations__to_concept on associations (to_concept);
create index idx_associations__observation_uuid on associations (observation_uuid);
create index idx_image_references__url on image_references (url);
create index idx_image_references__imaged_moment_uuid on image_references (imaged_moment_uuid);
alter table image_references
    add constraint UK_ikwbuple2jhdifw1dlsp5c4sn unique (url);
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
    add constraint fk_ancillary_data__imaged_moments foreign key (imaged_moment_uuid) references imaged_moments;
alter table associations
    add constraint fk_assocations__observations foreign key (observation_uuid) references observations;
alter table image_references
    add constraint fk_image_references__imaged_moments foreign key (imaged_moment_uuid) references imaged_moments;
alter table observations
    add constraint fk_observations__imaged_moments foreign key (imaged_moment_uuid) references imaged_moments;

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

 ALTER TABLE "dbo"."ancillary_data" WITH NOCHECK
	ADD CONSTRAINT "FK_ancillary_data_imaged_moment_uuid"
	FOREIGN KEY("imaged_moment_uuid")
	REFERENCES "dbo"."imaged_moments"("uuid")
	ON DELETE NO ACTION 
	ON UPDATE NO ACTION;

ALTER TABLE "dbo"."associations"
	ADD CONSTRAINT "FK_associations_observation_uuid"
	FOREIGN KEY("observation_uuid")
	REFERENCES "dbo"."observations"("uuid")
	ON DELETE NO ACTION 
	ON UPDATE NO ACTION;

ALTER TABLE "dbo"."image_references" WITH NOCHECK
	ADD CONSTRAINT "FK_image_references_imaged_moment_uuid"
	FOREIGN KEY("imaged_moment_uuid")
	REFERENCES "dbo"."imaged_moments"("uuid")
	ON DELETE NO ACTION 
	ON UPDATE NO ACTION;

ALTER TABLE "dbo"."observations"
	ADD CONSTRAINT "FK_observations_imaged_moment_uuid"
	FOREIGN KEY("imaged_moment_uuid")
	REFERENCES "dbo"."imaged_moments"("uuid")
	ON DELETE NO ACTION 
	ON UPDATE NO ACTION;



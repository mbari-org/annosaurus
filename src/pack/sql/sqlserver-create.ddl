-- ImagedMoment
CREATE TABLE imaged_moments (uuid VARCHAR(255) NOT NULL, elapsed_time_millis NUMERIC(19) NULL, last_updated_timestamp DATETIME2 NULL, recorded_timestamp DATETIME2 NULL, timecode VARCHAR(255) NULL, video_reference_uuid VARCHAR(255) NULL, PRIMARY KEY (uuid))
GO

CREATE INDEX idx_recorded_timestamp ON imaged_moments (recorded_timestamp)
GO

CREATE INDEX idx_elapsed_time ON imaged_moments (elapsed_time_millis)
GO

CREATE INDEX idx_timecode ON imaged_moments (timecode)
GO


-- Observation
CREATE TABLE observations (uuid VARCHAR(255) NOT NULL, activity VARCHAR(128) NULL, concept VARCHAR(256) NULL, duration_millis NUMERIC(19) NULL, observation_group VARCHAR(128) NULL, last_updated_timestamp DATETIME2 NULL, observation_timestamp DATETIME2 NULL, observer VARCHAR(128) NULL, imaged_moment_uuid VARCHAR(255) NOT NULL, PRIMARY KEY (uuid))
GO

CREATE INDEX idx_concept ON observations (concept)
GO

CREATE INDEX idx_observation_group ON observations (observation_group)
GO

CREATE INDEX idx_activity ON observations (activity)
GO


-- Association
CREATE TABLE associations (uuid VARCHAR(255) NOT NULL, last_updated_timestamp DATETIME2 NULL, link_name VARCHAR(128) NOT NULL, link_value VARCHAR(1024) NULL, to_concept VARCHAR(128) NULL, observation_uuid VARCHAR(255) NOT NULL, PRIMARY KEY (uuid))
GO

CREATE INDEX idx_link_name ON associations (link_name)
GO

CREATE INDEX idx_link_value ON associations (link_value)
GO

CREATE INDEX idx_to_concept ON associations (to_concept)
GO


-- ImageReference
CREATE TABLE image_references (uuid VARCHAR(255) NOT NULL, description VARCHAR(256) NULL, format VARCHAR(64) NULL, height_pixels INTEGER NULL, last_updated_timestamp DATETIME2 NULL, url VARCHAR(1024) NOT NULL UNIQUE, width_pixels INTEGER NULL, imaged_moment_uuid VARCHAR(255) NOT NULL, PRIMARY KEY (uuid))
GO

CREATE INDEX idx_url ON image_references (url)
GO


-- CachedAncillaryDatum
CREATE TABLE ancillary_data (uuid VARCHAR(255) NOT NULL, altitude FLOAT(16) NULL, coordinate_reference_system VARCHAR(32) NULL, depth_meters FLOAT(16) NULL, last_updated_timestamp DATETIME2 NULL, latitude FLOAT(32) NULL, longitude FLOAT(32) NULL, oxygen_ml_per_l FLOAT(16) NULL, phi FLOAT(32) NULL, xyz_position_units VARCHAR(255) NULL, pressure_dbar FLOAT(16) NULL, psi FLOAT(32) NULL, salinity FLOAT(16) NULL, temperature_celsius FLOAT(16) NULL, theta FLOAT(32) NULL, x FLOAT(32) NULL, y FLOAT(32) NULL, z FLOAT(32) NULL, imaged_moment_uuid VARCHAR(255) NOT NULL, PRIMARY KEY (uuid))
GO

CREATE INDEX idx_position ON ancillary_data (latitude, longitude, depth_meters)
GO

-- CachedVideoReferenceInfo
CREATE TABLE video_reference_information (uuid VARCHAR(255) NOT NULL, last_updated_timestamp DATETIME2 NULL, mission_contact VARCHAR(64) NULL, mission_id VARCHAR(256) NOT NULL, platform_name VARCHAR(64) NOT NULL, video_reference_uuid VARCHAR(255) NOT NULL UNIQUE, PRIMARY KEY (uuid))
GO


-- Foreign Keys
ALTER TABLE observations ADD CONSTRAINT FK_observations_imaged_moment_uuid FOREIGN KEY (imaged_moment_uuid) REFERENCES imaged_moments (uuid)
GO

ALTER TABLE associations ADD CONSTRAINT FK_associations_observation_uuid FOREIGN KEY (observation_uuid) REFERENCES observations (uuid)
GO

ALTER TABLE image_references ADD CONSTRAINT FK_image_references_imaged_moment_uuid FOREIGN KEY (imaged_moment_uuid) REFERENCES imaged_moments (uuid)
GO

ALTER TABLE ancillary_data ADD CONSTRAINT FK_ancillary_data_imaged_moment_uuid FOREIGN KEY (imaged_moment_uuid) REFERENCES imaged_moments (uuid)
GO

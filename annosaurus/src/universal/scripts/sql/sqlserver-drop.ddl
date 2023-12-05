-- Foreign Keys
ALTER TABLE ancillary_data DROP CONSTRAINT FK_ancillary_data_imaged_moment_uuid
GO

ALTER TABLE image_references DROP CONSTRAINT FK_image_references_imaged_moment_uuid
GO

ALTER TABLE associations DROP CONSTRAINT FK_associations_observation_uuid
GO

ALTER TABLE observations DROP CONSTRAINT FK_observations_imaged_moment_uuid
GO


-- CachedVideoReferenceInfo
DROP TABLE video_reference_information
GO


-- CachedAncillaryDatum
DROP INDEX ancillary_data.idx_position
GO

DROP TABLE ancillary_data
GO


-- ImageReference
DROP INDEX image_references.idx_url
GO

DROP TABLE image_references
GO


-- Association
DROP INDEX associations.idx_link_name
GO

DROP INDEX associations.idx_link_value
GO

DROP INDEX associations.idx_to_concept
GO

DROP TABLE associations
GO


-- Observation
DROP INDEX observations.idx_activity
GO

DROP INDEX observations.idx_observation_group
GO

DROP INDEX observations.idx_concept
GO

DROP TABLE observations
GO


-- ImagedMoment
DROP INDEX imaged_moments.idx_timecode
GO

DROP INDEX imaged_moments.idx_elapsed_time
GO

DROP INDEX imaged_moments.idx_recorded_timestamp
GO

DROP TABLE imaged_moments
GO

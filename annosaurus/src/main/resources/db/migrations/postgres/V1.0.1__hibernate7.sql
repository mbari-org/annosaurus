DROP VIEW annotations;

ALTER TABLE observations
    ALTER COLUMN observation_timestamp
        SET DATA TYPE timestamptz
    USING observation_timestamp AT TIME ZONE 'UTC';

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
    NULLIF(CONCAT_WS(' | ', ass.link_name, ass.to_concept, ass.link_value), '') AS associations,
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

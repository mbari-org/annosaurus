
CREATE VIEW [dbo].[annotations]
AS
    SELECT TOP 100 PERCENT
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
        ass.link_name + ' | ' + ass.to_concept + ' | ' + ass.link_value AS associations,
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
        vr.uuid AS video_reference_uuid,
        vr.audio_codec,
        vr.container AS video_container,
        vr.description AS video_reference_description,
        vr.frame_rate,
        vr.height AS video_height,
        vr.sha512 AS video_sha512,
        vr.size_bytes AS video_size_bytes,
        vr.uri AS video_uri,
        vr.video_codec,
        vr.width AS video_width,
        v.description AS video_description,
        v.duration_millis AS video_duration_millis,
        v.name AS video_name,
        v.start_time AS video_start_timestamp,
        vs.camera_id,
        vs.description AS video_sequence_description,
        vs.name AS video_sequence_name,
        info.mission_contact AS chief_scientist,
        info.mission_id AS dive_number,
        info.platform_name AS camera_platform
    FROM
        imaged_moments AS im
        LEFT JOIN observations AS obs ON obs.imaged_moment_uuid = im.uuid
        LEFT JOIN image_references AS ir ON ir.imaged_moment_uuid = im.uuid
        LEFT JOIN associations AS ass ON ass.observation_uuid = obs.uuid
        LEFT JOIN ancillary_data AS ad ON ad.imaged_moment_uuid = im.uuid
        LEFT JOIN [M3_VIDEO_ASSETS].[dbo].[video_references] AS vr ON vr.uuid = im.video_reference_uuid
        LEFT JOIN [M3_VIDEO_ASSETS].[dbo].[videos] AS v ON v.uuid = vr.video_uuid
        LEFT JOIN [M3_VIDEO_ASSETS].[dbo].[video_sequences] AS vs ON vs.uuid = v.video_sequence_uuid
        LEFT JOIN video_reference_information AS info ON info.video_reference_uuid = im.video_reference_uuid
GO

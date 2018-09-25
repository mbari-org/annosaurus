-- Created by Miroslaw Ryba (CSIRO) on 2018-02-26

as user VARS_VA2018":

grant select on VIDEOS           to VARS2018;
grant select on VIDEO_REFERENCES to VARS2018;
grant select on VIDEO_SEQUENCES  to VARS2018;



as user VARS2018:

CREATE OR REPLACE VIEW ANNOTATIONS
AS
SELECT IM.uuid                 AS imaged_moment_uuid,
       IM.elapsed_time_millis  AS index_elapsed_time_millis,
       IM.recorded_timestamp   AS index_recorded_timestamp,
       IM.timecode             AS index_timecode,
       OBS.uuid                AS observation_uuid,
       OBS.activity,
       OBS.concept,
       OBS.duration_millis,
       OBS.observation_group,
       OBS.observation_timestamp,
       OBS.observer,
       IR.uuid                 AS image_reference_uuid,
       IR.description          AS image_description,
       IR.format               AS image_format,
       IR.height_pixels        AS image_height,
       IR.width_pixels         AS image_width,
       IR.url                  AS image_url,
       ASS.link_name,
       ASS.link_value,
       ASS.to_concept,
       ASS.mime_type           AS association_mime_type,
       ASS.link_name || ' | ' || ASS.to_concept || ' | ' || ASS.link_value  AS associations,
       AD.altitude,
       AD.coordinate_reference_system,
       AD.depth_meters,
       AD.latitude,
       AD.longitude,
       AD.oxygen_ml_per_l,
       AD.phi,
       AD.xyz_position_units,
       AD.pressure_dbar,
       AD.psi,
       AD.salinity,
       AD.temperature_celsius,
       AD.theta,
       AD.x,
       AD.y,
       AD.z,
       AD.light_transmission,
       VR.uuid                 AS video_reference_uuid,
       VR.audio_codec,
       VR.container            AS video_container,
       VR.description          AS video_reference_description,
       VR.frame_rate,
       VR.height               AS video_height,
       VR.sha512               AS video_sha512,
       VR.size_bytes           AS video_size_bytes,
       VR.uri                  AS video_uri,
       VR.video_codec,
       VR.width                AS video_width,
       V.description           AS video_description,
       V.duration_millis       AS video_duration_millis,
       V.name                  AS video_name,
       V.start_time            AS video_start_timestamp,
       VS.camera_id,
       VS.description          AS camera_deployment_description,
       VS.name                 AS camera_deployment_id,
       INFO.mission_contact    AS chief_scientist,
       INFO.mission_id         AS mission_id,
       INFO.platform_name      AS camera_platform
FROM   imaged_moments   IM
       LEFT JOIN observations      OBS ON OBS.imaged_moment_uuid = IM.uuid
       LEFT JOIN image_references  IR  ON IR.imaged_moment_uuid  = IM.uuid
       LEFT JOIN associations      ASS ON ASS.observation_uuid   = OBS.uuid
       LEFT JOIN ancillary_data    AD  ON AD.imaged_moment_uuid  = IM.uuid
       LEFT JOIN VARS_VA2018.video_references  VR  ON   VR.uuid  = IM.video_reference_uuid
       LEFT JOIN VARS_VA2018.videos             V  ON    V.uuid  = VR.video_uuid
       LEFT JOIN VARS_VA2018.video_sequences   VS  ON   VS.uuid  = V.video_sequence_uuid
       LEFT JOIN video_reference_information INFO  ON INFO.video_reference_uuid  =  IM.video_reference_uuid
/

   




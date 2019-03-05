
CREATE VIEW [dbo].[annotations_legacy]
AS
  SELECT TOP 100 PERCENT
    obs.observation_timestamp AS ObservationDate,
    obs.observer AS Observer,
    obs.concept AS ConceptName,
    NULL AS Notes,
    NULL AS xPixelInImage,
    NULL AS yPixelInImage,
    im.timecode AS TapeTimeCode,
    im.recorded_timestamp AS RecordedDate,
    vr.uri AS videoArchiveName,
    NULL AS TrackingNumber,
    NULL AS ShipName,
    vs.camera_id AS RovName,
    obs.observation_group AS AnnotationMode,
    SUBSTRING(vs.name , LEN(vs.name) -  CHARINDEX(' ',REVERSE(vs.name)) + 2  , LEN(vs.name) ) AS DiveNumber,
    info.mission_contact AS ChiefScientist,
    NULL AS CameraName,
    obs.activity AS CameraDirection,
    NULL AS Zoom,
    NULL AS Focus,
    NULL AS Iris,
    NULL AS FieldWidth,
    ir.url AS Image,
    ad.x AS CameraX,
    ad.y AS CameraY,
    ad.z AS CameraZ,
    ad.theta AS CameraPitchRadians,
    ad.phi AS CameraRollRadians,
    ad.psi AS CameraHeadingRadians,
    ad.xyz_position_units AS CameraXYUnits,
    ad.xyz_position_units AS CameraZUnits,
    vr.height AS CameraViewHeight,
    vr.width AS CameraViewWidth,
    'pixels' AS CameraViewUnits,
    ad.depth_meters AS DEPTH,
    ad.temperature_celsius AS Temperature,
    ad.salinity AS Salinity,
    ad.oxygen_ml_per_l AS Oxygen,
    ad.light_transmission AS Light,
    ad.latitude AS Latitude,
    ad.longitude AS Longitude,
    ad.altitude AS Altitude,
    obs.uuid AS ObservationID_FK,
    ass.uuid AS AssociationID_FK,
    ass.link_name AS LinkName,
    ass.to_concept AS ToConcept,
    ass.link_value AS LinkValue,
    ass.link_name + ' | ' + ass.to_concept + ' | ' + ass.link_value AS Associations,
    im.timecode AS AlternateTimecode,
    NULL AS IsNavigationEdited,
    NULL AS IsMerged,
    im.uuid AS VideoFrameID_FK,
    ad.uuid AS PhysicalDataID_FK,
    ad.uuid AS CameraDataID_FK,
    vr.uuid AS VideoArchiveID_FK,
    vs.uuid AS VideoArchiveSetID_FK
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

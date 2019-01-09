CREATE VIEW [dbo].[observation_counts]
AS
    with
        observation_count_view
        AS
        (
            SELECT
                video_reference_uuid,
                COUNT(video_reference_uuid) AS counts
            FROM
                imaged_moments im LEFT JOIN
                observations obs ON obs.imaged_moment_uuid = im.uuid
            GROUP BY
                im.video_reference_uuid
        )
    SELECT
        vr.uuid AS video_reference_uuid,
        vr.uri AS video_uri,
        ISNULL(oc.counts, 0) AS observation_counts,
        v.start_time,
        v.name AS video_name,
        vs.name AS video_sequence_name

    FROM
        [M3_VIDEO_ASSETS].[dbo].[video_references] vr LEFT JOIN
        observation_count_view oc ON oc.video_reference_uuid = vr.uuid LEFT JOIN
        [M3_VIDEO_ASSETS].[dbo].[videos] v ON v.uuid = vr.video_uuid LEFT JOIN
        [M3_VIDEO_ASSETS].[dbo].[video_sequences] vs ON vs.uuid = v.video_sequence_uuid
GO

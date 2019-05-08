SELECT
    camera_id,
    video_year AS year,
    SUM(duration_minutes) / 60 AS total_video_duration_hours,
    SUM(size_gigabytes) as total_video_size_gb, 
    COUNT(*) AS video_count,
    SUM(duration_minutes) / COUNT(*) AS avg_video_duration_minutes,
    SUM(size_gigabytes) / (SUM(duration_minutes) / 60) AS gb_per_hour
FROM 
(
SELECT 
    uri,
    start_time, 
    duration_millis / 1000 / 60 AS duration_minutes,
    camera_id, 
    size_bytes * 1e-9 AS size_gigabytes,
    YEAR(start_time) AS video_year
FROM
    unique_videos
WHERE
    uri LIKE 'http%mp4'
) foo
GROUP BY camera_id, video_year
ORDER BY video_year, camera_id
GO

SELECT
    camera_id,
    video_year AS year,
    SUM(duration_minutes) / 60 AS total_video_duration_hours,
    SUM(size_gigabytes) as total_video_size_gb, 
    COUNT(*) AS video_count,
    SUM(duration_minutes) / COUNT(*) AS avg_video_duration_minutes,
    SUM(size_gigabytes) / (SUM(duration_minutes) / 60) AS gb_per_hour
FROM 
(
SELECT 
    uri,
    start_time, 
    duration_millis / 1000 / 60 AS duration_minutes,
    camera_id, 
    size_bytes * 1e-9 AS size_gigabytes,
    YEAR(start_time) AS video_year
FROM
    unique_videos
WHERE
    uri LIKE 'http%mov' 
) foo
GROUP BY camera_id, video_year
ORDER BY video_year, camera_id
GO
# Fetching annotations

There are a number of endpoints for fetching annotations. The recommended way to fetch annotations is to use the `GET /v1/fast` endpoints.

By default, he endpoints below do no include the ancillary data for each annotation. To include the ancillary data, add the `?data=true` query parameter to the URL.

## Fetch by video

Get all annotations for a single video. You will need the `video_reference_uuid` from your video asset manager.

`GET http://myserver.org/anno/v1/fast/videoreference/<video_reference_uuid>`

## Fetch by multiple videos

Get all annotations for multiple videos. You will need the list of `video_reference_uuid` from your video asset manager.

```text
POST http://myserver.org/anno/v1/fast/multi

Content-Type: application/json

{
  "video_reference_uuids": [
    "<video_reference_uuid>",
    "<video_reference_uuid>",
    "<video_reference_uuid>"
  ]
}
```

## Fetch by multiple videos but only within specific time bounds

Gets all annotations from multiple videos, but only returns the ones in the given time range.

```text
POST http://myserver.org/anno/v1/fast/concurrent

Content-Type: application/json

{
  "video_reference_uuids": [
    "<video_reference_uuid>",
    "<video_reference_uuid>",
    "<video_reference_uuid>"
  ],
  "start_timestamp": <start_time_iso8601>,
  "end_timestamp": <end_time_iso8601>
}
```

iso8601 timestamps are formated like: `yyyy-mm-ddThh:mm:ss.sssZ`

## Fetch by concept name

Gets all annotations for a concept name.

```text
GET http://myserver.org/anno/v1/fast/concept/<concept_name>
```

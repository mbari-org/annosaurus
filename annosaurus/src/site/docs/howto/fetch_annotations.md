# Fetching Annotations

There are a number of endpoints for fetching annotations. The recommended approach is to use the `GET /v1/fast` endpoints.

By default, the endpoints below do not include ancillary data for each annotation. To include it, add the `?data=true` query parameter.

## Fetch by video

Get all annotations for a single video. You will need the `video_reference_uuid` from your video asset manager.

```text
GET http://myserver.org/anno/v1/fast/videoreference/<video_reference_uuid>
```

## Fetch by multiple videos

Get all annotations for a set of videos.

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

## Fetch by multiple videos within a time window

Returns only annotations whose `recordedTimestamp` falls within the given range.

```text
POST http://myserver.org/anno/v1/fast/concurrent
Content-Type: application/json

{
  "video_reference_uuids": [
    "<video_reference_uuid>",
    "<video_reference_uuid>",
    "<video_reference_uuid>"
  ],
  "start_timestamp": "<ISO 8601>",
  "end_timestamp": "<ISO 8601>"
}
```

Timestamps must be ISO 8601 formatted: `YYYY-MM-DDTHH:MM:SS.sssZ`

## Fetch by concept name

Get all annotations for a given concept name.

```text
GET http://myserver.org/anno/v1/fast/concept/<concept_name>
```

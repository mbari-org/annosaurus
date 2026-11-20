# Point localization: create, edit, delete

A single location, normalized to [0, 1] over the frame, top-left origin.

An observation can have **at most one** localization of each type. The pair (observation, type) is the identity, so create and edit are the same call (`PUT`), and delete addresses the same pair. All coordinates are normalized to [0, 1] with the origin at the top-left of the frame. Every call needs `Authorization: Bearer <jwt>`.

## Rules

- Required: `x`, `y`, both in [0, 1].
- `width` and `height` may be omitted; if sent they must be `0`. The stored (and returned) extent is exactly `0` x `0`.
- As an alternative to `x`/`y`, a point may be sent as a single vertex pair, `"vertices": [x, y]`. The server converts it to the canonical `x`/`y` form and the response never contains `vertices`. Sending more than one pair, or both `x`/`y` and `vertices`, is a `400`.
- Do not send `vertices` (rejected) or `mask_*` (ignored).

### Optional fields (all types)

| Field | Notes |
|---|---|
| `generator` | Who or what produced it (model name, tool), max 128 characters. |
| `confidence` | 0 to 1. Values outside that range are rejected. |
| `observer`, `verifier` | Max 128 characters. |
| `observation_timestamp`, `verification_timestamp` | ISO-8601 instants. |
| `part` | Free text naming the part of the organism, max 128 characters. |
| `comment` | Max 1024 characters. |
| `training`, `truncated`, `difficult` | Booleans, default `false`. |
| `last_updated_timestamp` | Stale-write token, see **Edit**. |

## Create

```
PUT /v1/localizations/{observationUuid}/point
Content-Type: application/json
```

`{observationUuid}` is the observation (annotation) to attach it to. The path decides the type, but the body must still contain `localization_type`.

```json
{
  "localization_type": "point",
  "x": 0.5,
  "y": 0.25,
  "generator": "yolo-v11",
  "confidence": 0.87,
  "observer": "brian",
  "part": "eye"
}
```

Response `200 OK`: the stored localization, with the derived fields filled in. `uuid` and `last_updated_time` are server-generated (the values here are illustrative). Unset fields are omitted.

```json
{
  "localization_type": "point",
  "x": 0.5,
  "y": 0.25,
  "width": 0.0,
  "height": 0.0,
  "generator": "yolo-v11",
  "confidence": 0.87,
  "observer": "brian",
  "part": "eye",
  "training": false,
  "truncated": false,
  "difficult": false,
  "uuid": "6f1c2a52-8d0e-4b1f-9a77-3c5e0d7b1a10",
  "observation_uuid": "0b8d7c9e-41a2-4f6b-8e35-2d9a6c1f4e77",
  "last_updated_time": "2026-10-29T20:15:42.318Z"
}
```

The same object can instead be placed in the `localizations` array of `POST /v1/annotations` or `POST /v1/annotations/bulk` when the observation is created (omit `observation_uuid` there), or sent with an `observation_uuid` to `POST /v1/localizations/bulk` (all or nothing; failing indexes are named in the 400).

## Edit

Edit is the same `PUT /v1/localizations/{observationUuid}/point`. It **replaces** the stored localization: every field you leave out is cleared (flags go back to `false`), including provenance such as `generator` and `confidence`. Always send the complete body, not just what changed. The localization keeps its `uuid`.

To guard against overwriting someone else's change, send the `last_updated_time` you last read back as `last_updated_timestamp` (`last_updated_time` is also accepted). If it does not match the stored row, nothing is written and the server returns `409` with the current row. Omit it to overwrite unconditionally.

Here the point is moved down (`y` 0.25 to 0.375).

```json
{
  "localization_type": "point",
  "x": 0.5,
  "y": 0.375,
  "generator": "yolo-v11",
  "confidence": 0.87,
  "observer": "brian",
  "part": "eye",
  "last_updated_timestamp": "2026-10-29T20:15:42.318Z"
}
```

Response `200 OK`:

```json
{
  "localization_type": "point",
  "x": 0.5,
  "y": 0.375,
  "width": 0.0,
  "height": 0.0,
  "generator": "yolo-v11",
  "confidence": 0.87,
  "observer": "brian",
  "part": "eye",
  "training": false,
  "truncated": false,
  "difficult": false,
  "uuid": "6f1c2a52-8d0e-4b1f-9a77-3c5e0d7b1a10",
  "observation_uuid": "0b8d7c9e-41a2-4f6b-8e35-2d9a6c1f4e77",
  "last_updated_time": "2026-10-29T20:31:07.915Z"
}
```

Stale-write response `409 Conflict`:

```json
{
  "message": "...",
  "responseCode": 409,
  "current": { "...": "the stored localization as it is now" }
}
```

Re-read `current`, reapply your change, and retry with its `last_updated_time`.

## Delete

```
DELETE /v1/localizations/{observationUuid}/point
```

Response `200 OK` with an empty body. If the observation has no point localization the response is `404`. Deleting an observation, or its video reference, removes its localizations too. Other types on the same observation are not affected.

To list what an observation has first, `GET /v1/localizations/observation/{observationUuid}`; to fetch one by its uuid, `GET /v1/localizations/{localizationUuid}`.

## Errors

| Status | When |
|---|---|
| `400` | Validation failed. Examples: a non-zero `width` or `height` gives 400 `point width must be absent or 0`. The message lists every problem found. |
| `401` | Missing or invalid bearer token. |
| `404` | The observation does not exist (create/edit), or there is nothing to delete. |
| `409` | Stale `last_updated_timestamp` on edit, or another writer created the same (observation, type) first. The body carries `current`. |

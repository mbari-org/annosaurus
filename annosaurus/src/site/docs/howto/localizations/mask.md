# Mask (COCO RLE) localization: create, edit, delete

A pixel segmentation mask covering the whole frame, sent as a compressed COCO run-length encoding (the pycocotools `counts` string). Pixels are column-major and runs alternate starting with background. See [mask-example.md](mask-example.md) for how to produce the string from Python and from SAM 3 output.

An observation can have **at most one** localization of each type. The pair (observation, type) is the identity, so create and edit are the same call (`PUT`), and delete addresses the same pair. All coordinates are normalized to [0, 1] with the origin at the top-left of the frame. Every call needs `Authorization: Bearer <jwt>`.

## Rules

- Required: `mask_encoding` (`coco_rle`, the only value), `mask_data` (compressed counts string), `mask_width_pixels` and `mask_height_pixels` (both > 0). The size is the pycocotools `size` as `[height, width]`.
- The decoded runs must add up to exactly width x height pixels.
- Do not send `vertices` (rejected). Any `x`/`y`/`width`/`height` you send is overwritten: the server derives the bounding box of the set pixels, normalized by the raster size.
- `mask_data` can be large (hundreds of KB for a big object); there is currently no request size limit.

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
PUT /v1/localizations/{observationUuid}/mask
Content-Type: application/json
```

`{observationUuid}` is the observation (annotation) to attach it to. The path decides the type, but the body must still contain `localization_type`.

```json
{
  "localization_type": "mask",
  "mask_encoding": "coco_rle",
  "mask_data": "e13700000000000]2",
  "mask_width_pixels": 20,
  "mask_height_pixels": 10,
  "generator": "sam2",
  "confidence": 0.93,
  "observer": "brian",
  "part": "tentacle"
}
```

Response `200 OK`: the stored localization, with the derived fields filled in. `uuid` and `last_updated_time` are server-generated (the values here are illustrative). Unset fields are omitted.

```json
{
  "localization_type": "mask",
  "x": 0.25,
  "y": 0.3,
  "width": 0.35,
  "height": 0.3,
  "mask_encoding": "coco_rle",
  "mask_data": "e13700000000000]2",
  "mask_width_pixels": 20,
  "mask_height_pixels": 10,
  "generator": "sam2",
  "confidence": 0.93,
  "observer": "brian",
  "part": "tentacle",
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

Edit is the same `PUT /v1/localizations/{observationUuid}/mask`. It **replaces** the stored localization: every field you leave out is cleared (flags go back to `false`), including provenance such as `generator` and `confidence`. Always send the complete body, not just what changed. The localization keeps its `uuid`.

To guard against overwriting someone else's change, send the `last_updated_time` you last read back as `last_updated_timestamp` (`last_updated_time` is also accepted). If it does not match the stored row, nothing is written and the server returns `409` with the current row. Omit it to overwrite unconditionally.

Send the complete mask again, including the new `mask_data`, the size, and the token. The response has the same shape as Create, with a re-derived box and a new `last_updated_time`.

```json
{
  "localization_type": "mask",
  "mask_encoding": "coco_rle",
  "mask_data": "<new counts string>",
  "mask_width_pixels": 20,
  "mask_height_pixels": 10,
  "generator": "sam2",
  "confidence": 0.93,
  "observer": "brian",
  "part": "tentacle",
  "last_updated_timestamp": "2026-10-29T20:15:42.318Z"
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
DELETE /v1/localizations/{observationUuid}/mask
```

Response `200 OK` with an empty body. If the observation has no mask localization the response is `404`. Deleting an observation, or its video reference, removes its localizations too. Other types on the same observation are not affected.

To list what an observation has first, `GET /v1/localizations/observation/{observationUuid}`; to fetch one by its uuid, `GET /v1/localizations/{localizationUuid}`.

## Errors

| Status | When |
|---|---|
| `400` | Validation failed. Examples: `mask_data` that is not valid compressed RLE, or whose runs do not match the size gives 400 `mask maskData is not a valid coco_rle mask of 20x10 pixels: ...`. The message lists every problem found. |
| `401` | Missing or invalid bearer token. |
| `404` | The observation does not exist (create/edit), or there is nothing to delete. |
| `409` | Stale `last_updated_timestamp` on edit, or another writer created the same (observation, type) first. The body carries `current`. |

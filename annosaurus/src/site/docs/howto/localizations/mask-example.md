# Adding a mask localization (COCO RLE)

How to send a segmentation mask to annosaurus. The only supported `mask_encoding` is `coco_rle`, the
**compressed** pycocotools `counts` string (not the uncompressed integer-list form).

## Request

```
PUT /v1/localizations/{observationUuid}/mask
Authorization: Bearer <jwt>
Content-Type: application/json
```

The example is a 20 x 10 pixel raster with a 7 x 3 block of set pixels whose top-left corner is at x=5, y=3.
The `counts` string was generated with pycocotools.

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
  "training": true,
  "part": "tentacle"
}
```

### Fields

| Field | Required | Notes |
|---|---|---|
| `localization_type` | yes | Must be repeated in the body even though it is in the URL path (`mask`). |
| `mask_encoding` | yes | `coco_rle` |
| `mask_data` | yes | pycocotools `counts` string. Column-major, runs alternate starting with background. |
| `mask_width_pixels` | yes | pycocotools `size[1]`. Must be > 0. |
| `mask_height_pixels` | yes | pycocotools `size[0]`. Must be > 0. |
| `generator`, `confidence`, `observer`, `part`, `comment` | no | `confidence` is 0-1. |
| `training`, `truncated`, `difficult` | no | Booleans, default `false`. |
| `last_updated_timestamp` | no | Stale-write token. Must match the stored row or the server returns 409 with the current row. |

Do **not** send `vertices`. Any `x`, `y`, `width` or `height` you send for a mask is ignored and replaced:
the server derives the bounding box from the decoded mask. The decoded runs must add up to exactly
`mask_width_pixels` x `mask_height_pixels`, otherwise the request is rejected with 400.

### Producing the request body from Python

```python
import numpy as np
from pycocotools import mask as m

a = np.zeros((10, 20), dtype=np.uint8, order="F")   # (height, width)
a[3:6, 5:12] = 1
rle = m.encode(a)
body = {
    "localization_type": "mask",
    "mask_encoding": "coco_rle",
    "mask_data": rle["counts"].decode(),             # counts is bytes
    "mask_width_pixels": rle["size"][1],
    "mask_height_pixels": rle["size"][0],
}
```

### Masks from SAM 3

Meta's SAM 3 (`facebookresearch/sam3`) can emit the same format directly. By default it returns binary masks as a
boolean tensor of shape `(N, H, W)` (`masks`). With `convert_mask_to_rle` set it returns `masks_rle` instead: a list
of `{"counts": "<compressed string>", "size": [H, W]}` items. These are column-major, compressed pycocotools RLE,
so no conversion is needed:

| SAM 3 `masks_rle` item | annosaurus field |
|---|---|
| `counts` | `mask_data` |
| `size[1]` (W) | `mask_width_pixels` |
| `size[0]` (H) | `mask_height_pixels` |

Notes:

- `size` is `[H, W]`; height comes first.
- The mask is sized to the image SAM 3 was given. If it ran on a resized frame, `size` is that resized resolution,
  which is fine: annosaurus normalizes by the mask's own raster size.
- Send `counts` as a string. Third-party wrappers (Roboflow, fal.ai) may return polygons or a different RLE form;
  check that `counts` is the compressed string, otherwise the request fails with "Invalid RLE character".
- If you only have the boolean tensor, encode it with pycocotools using Fortran order, as in the Python snippet above.

## Response

`200 OK` with the stored localization. The bounding box (`x`, `y`, `width`, `height`) is normalized to [0, 1]
over the mask raster (top-left origin): x = 5/20, y = 3/10, width = 7/20, height = 3/10.
`uuid`, `observation_uuid` and `last_updated_time` are generated or filled in by the server; the values below are
illustrative. Unset fields are omitted.

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
  "training": true,
  "truncated": false,
  "difficult": false,
  "uuid": "6f1c2a52-8d0e-4b1f-9a77-3c5e0d7b1a10",
  "observation_uuid": "0b8d7c9e-41a2-4f6b-8e35-2d9a6c1f4e77",
  "last_updated_time": "2026-10-29T20:15:42.318Z"
}
```

## Other ways to send a mask

The same object can be placed in the `localizations` array of `POST /v1/annotations` or
`POST /v1/annotations/bulk`; omit `observation_uuid` there. An annotation may carry at most one localization of
each type.

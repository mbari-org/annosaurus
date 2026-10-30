# Localizations

A localization is a shape drawn on the frame of an observation: a box, point, line, polygon or pixel mask. One page per type covers how to create, edit and delete it.

An observation can have **at most one** localization of each type. All coordinates are normalized to [0, 1] with the origin at the top-left of the frame, and every call needs `Authorization: Bearer <jwt>`.

| Type | Page |
|---|---|
| `bounding_box` | [Bounding box](bounding-box.md) |
| `point` | [Point](point.md) |
| `line` | [Line](line.md) |
| `polygon` | [Polygon](polygon.md) |
| `mask` | [Mask](mask.md) |

## Reading localizations back

Observation responses carry their localizations in a `localizations` array, alongside `associations`. An observation with no localizations returns an empty array. Annotation responses include them too.

```json
{
  "concept": "Grimpoteuthis",
  "observer": "brian",
  "associations": [],
  "localizations": [
    {
      "localization_type": "point",
      "x": 0.5,
      "y": 0.25,
      "width": 0.0,
      "height": 0.0,
      "training": false,
      "truncated": false,
      "difficult": false,
      "uuid": "6f1c2a52-8d0e-4b1f-9a77-3c5e0d7b1a10",
      "observation_uuid": "0b8d7c9e-41a2-4f6b-8e35-2d9a6c1f4e77",
      "last_updated_time": "2026-10-29T20:15:42.318Z"
    }
  ],
  "uuid": "0b8d7c9e-41a2-4f6b-8e35-2d9a6c1f4e77"
}
```

To list one observation's localizations directly, use `GET /v1/localizations/observation/{observationUuid}`.

See also [Mask example](mask-example.md) for producing COCO RLE strings from Python and SAM 3 output.

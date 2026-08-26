# Advanced Queries

The `/v1/query` endpoints let you query annotations the way you would query a table: pick the columns you want, filter on any of them, and get a tab-separated file back. They are the right tool for assembling a dataset for analysis, and they can express filters that the [fast annotation endpoints](fetch_annotations.md) cannot.

The typical workflow is two steps:

1. `GET /v1/query/columns` — discover what you can select and filter on.
2. `POST /v1/query/download` — submit a query and download the results as TSV.

`POST /v1/query/count` is a useful third step between the two: it tells you how many rows a query will return before you commit to downloading them.

None of these endpoints require authentication — they are read-only.

> **There is also `/v1/query/run`.** It accepts the same request body and returns TSV in the response body instead of as a file attachment. It builds the entire result set in memory before responding, so it is slower and far more memory-hungry than `/v1/query/download`, which streams rows to a file as it reads them. Prefer `/v1/query/download`.

## Everything queries a single view

These endpoints do not query the annotation tables directly. They query one flat database view — named `annotations` by default, overridable with `DATABASE_QUERY_VIEW` — that joins imaged moments, observations, associations, image references, ancillary data, and video reference info into a single wide, denormalized row set.

Two consequences matter:

- **One row is not one annotation.** Because the view is a join across several one-to-many relationships, an observation with three associations and two framegrabs appears as six rows. Use `distinct`, or select only the columns you actually need, to control this.
- **The available columns depend on the deployment.** The view is created by a migration and can be customized per site, so the column list is not part of the API contract. Always ask `/v1/query/columns` rather than hard-coding column names.

## Step 1: discover the columns

```text
GET http://myserver.org/anno/v1/query/columns
```

The response is a JSON array of column descriptors, one per column in the view:

```json
[
  {
    "columnName": "index_recorded_timestamp",
    "columnType": "timestamptz",
    "columnSize": 35,
    "columnLabel": "index_recorded_timestamp",
    "columnClassName": "java.sql.Timestamp"
  },
  {
    "columnName": "concept",
    "columnType": "varchar",
    "columnSize": 256,
    "columnLabel": "concept",
    "columnClassName": "java.lang.String"
  },
  {
    "columnName": "depth_meters",
    "columnType": "float4",
    "columnSize": 15,
    "columnLabel": "depth_meters",
    "columnClassName": "java.lang.Float"
  }
]
```

`columnName` is what you put in `select`, `where`, and `orderBy`. `columnClassName` is the useful field for building a request, because **which constraints are legal depends on the column's type** — see [Choosing the right constraint](#choosing-the-right-constraint).

The default view provides these columns, grouped by where they come from:

| Source | Columns |
| --- | --- |
| Imaged moment | `imaged_moment_uuid`, `index_recorded_timestamp`, `index_elapsed_time_millis`, `index_timecode` |
| Observation | `observation_uuid`, `concept`, `observer`, `activity`, `observation_group`, `observation_timestamp`, `duration_millis` |
| Association | `link_name`, `to_concept`, `link_value`, `association_mime_type`, `associations` |
| Image reference | `image_reference_uuid`, `image_url`, `image_format`, `image_width`, `image_height`, `image_description` |
| Ancillary data | `latitude`, `longitude`, `depth_meters`, `altitude`, `salinity`, `temperature_celsius`, `oxygen_ml_per_l`, `pressure_dbar`, `light_transmission`, `coordinate_reference_system`, `x`, `y`, `z`, `phi`, `theta`, `psi`, `xyz_position_units` |
| Video reference info | `camera_platform`, `dive_number`, `chief_scientist` |

The column list above is the PostgreSQL view. The SQL Server view is nearly identical but names the concatenated association column `association` rather than `associations`, and adds an `association_uuid` column — another reason to read the column list from the server instead of assuming it.

Note that `video_reference_uuid` is *not* in the default view. Filter by `dive_number` or `camera_platform` instead, or use the [fast endpoints](fetch_annotations.md) when you already have a video reference UUID.

## Step 2: build the request

The request body is the same for `download`, `count`, and `run`. Field names are camelCase; column names are the snake_case names from `/v1/query/columns`.

```json
{
  "select": ["concept", "index_recorded_timestamp", "depth_meters"],
  "distinct": false,
  "where": [
    { "column": "concept", "equals": "Nanomia bijuga" },
    { "column": "depth_meters", "minmax": [200, 800] }
  ],
  "orderBy": ["index_recorded_timestamp"],
  "limit": 5000,
  "offset": 0,
  "concurrentObservations": false,
  "relatedAssociations": false,
  "strict": true
}
```

| Field | Meaning |
| --- | --- |
| `select` | Columns to return. **Required** for `download` and `run`; ignored by `count`. |
| `distinct` | Emit `SELECT DISTINCT`. Default `false`. |
| `where` | Constraints, combined with `AND`. **Required** by `count`, and by `download`/`run` when you explicitly set `"strict": true`. |
| `orderBy` | Columns to sort by, always ascending. Defaults to the first column in `select`. |
| `limit` / `offset` | Page through results. |
| `concurrentObservations` | Also return the other observations recorded at the same imaged moment. Default `false`. |
| `relatedAssociations` | Also return the other associations belonging to matched observations. Default `false`. |
| `strict` | Whether to return *only* the columns you selected. Default `true`. |

All constraints in `where` are ANDed together. There is no `OR` and no nesting; for alternatives on a single column use `in`.

### Choosing the right constraint

Each object in `where` names a `column` plus **exactly one** constraint. If you supply more than one, only the first is used, in the order listed below — the rest are silently ignored. Split them into separate `where` entries instead.

| Constraint | JSON type | SQL | Use on |
| --- | --- | --- | --- |
| `between` | array of 2 ISO-8601 instants | `col BETWEEN ? AND ?` | timestamp columns |
| `contains` | string | `col LIKE '%value%'` | text columns |
| `equals` | string | `col = ?` | text and UUID columns |
| `in` | array of strings | `col IN (?, …)` | text and UUID columns |
| `isnull` | boolean | `col IS NULL` / `IS NOT NULL` | any column |
| `like` | string | `col LIKE ?` | text columns |
| `notlike` | string | `col NOT LIKE ?` | text columns |
| `max` | number | `col <= ?` | numeric columns |
| `min` | number | `col >= ?` | numeric columns |
| `minmax` | array of 2 numbers | `col BETWEEN ? AND ?` | numeric columns |

Matching the constraint to the column type matters, because a mismatch reaches the database as a type error rather than an empty result:

- **Numeric columns** (`columnClassName` of `java.lang.Float`, `java.lang.Double`, `java.lang.Integer`, `java.math.BigDecimal`) take `min`, `max`, and `minmax`. Using `equals` or `like` on one fails — the value is sent as a string, and the database rejects `integer = character varying`.
- **Text columns** (`java.lang.String`) take `equals`, `in`, `like`, `notlike`, and `contains`. Using `min`/`max`/`minmax` on one fails the same way, in reverse.
- **Timestamp columns** (`java.sql.Timestamp`) take `between`, with exactly two ISO-8601 values. `between` is timestamps only — use `minmax` for numeric ranges.
- **UUID columns** (`java.util.UUID`) work with `equals` and `in`, passing the UUID as a string.
- `isnull` works on any column.

With `like` and `notlike` you supply the `%` wildcards yourself; `contains` wraps the value in `%` for you.

### `strict`, and the two expansion flags

`strict` controls whether the query returns exactly the columns you asked for:

- `"strict": true` (the default) returns precisely your `select` list, ordered by `select`'s first column unless you set `orderBy`.
- `"strict": false` prepends `observation_uuid` and `index_recorded_timestamp` to your `select` list and orders by those two. The extra columns let a client regroup flat rows back into annotations.

Setting `concurrentObservations` or `relatedAssociations` to `true` **forces `strict` to `false`**, whatever you passed in, because those results are meaningless without the grouping columns.

The two flags widen the result set rather than narrowing it. Your `where` clause becomes a subquery that selects matching rows, and then:

- `concurrentObservations` returns every row whose `imaged_moment_uuid` matched — all observations made at the same instant in the video, not just the ones matching your filter.
- `relatedAssociations` returns every row whose `observation_uuid` matched — all associations on the matched observations.
- Both together return all observations at the matched moments, along with all of their associations.

```json
{
  "select": ["concept", "link_name", "link_value", "index_recorded_timestamp"],
  "where": [{ "column": "concept", "equals": "Sebastes" }],
  "relatedAssociations": true
}
```

That query returns every association on every *Sebastes* observation, not only the rows where the association itself matched.

> **Note:** `distinct` is rarely useful with either flag set, since the forced `observation_uuid` column makes nearly every row unique.

## Step 3: check the size, then download

`count` runs your `where` clause and returns just the row count. It requires a `where` clause and ignores `select`:

```text
POST http://myserver.org/anno/v1/query/count
Content-Type: application/json

{
  "where": [
    { "column": "concept", "equals": "Nanomia bijuga" },
    { "column": "depth_meters", "minmax": [200, 800] }
  ]
}
```

```json
{ "count": 14203 }
```

Then download the rows:

```bash
curl -X POST 'http://myserver.org/anno/v1/query/download' \
  -H 'Content-Type: application/json' \
  -o nanomia.tsv \
  -d '{
    "select": ["concept", "index_recorded_timestamp", "depth_meters", "latitude", "longitude"],
    "where": [
      { "column": "concept", "equals": "Nanomia bijuga" },
      { "column": "depth_meters", "minmax": [200, 800] }
    ],
    "orderBy": ["index_recorded_timestamp"]
  }'
```

The response is a file attachment with `Content-Type: text/tab-separated-values` and `Content-Disposition: attachment; filename=results.tsv`. Give `curl` an `-o` of your own, as above, or use `-OJ` to accept the server's name — every download is named `results.tsv`, so `-OJ` will collide if you run more than one.

Notes on the output:

- The first line is a header of column names.
- Rows are ordered by `orderBy`, or by the defaults described under [`strict`](#strict-and-the-two-expansion-flags).
- `NULL` values are written as empty fields.
- Timestamps are rendered in UTC.
- Fields are **not** quoted or escaped. A `link_value` containing a tab or newline — free-text comments are the usual suspect — will break column alignment for that row. Exclude such columns, or post-process, when that matters.
- The server writes the file to temporary storage and deletes it about two minutes after responding. Read the response body to completion; there is no URL to come back to later.

## Worked examples

**All annotations for one dive, grouped back into annotations by the client**

```json
{
  "select": ["concept", "observer", "index_recorded_timestamp", "associations", "image_url"],
  "where": [{ "column": "video_sequence_name", "equals": "Doc Ricketts 1373" }],
  "strict": false
}
```

**Concept counts by platform over a date range** — download and aggregate locally

```json
{
  "select": ["camera_platform", "concept"],
  "where": [
    { "column": "index_recorded_timestamp", "between": ["2023-01-01T00:00:00Z", "2024-01-01T00:00:00Z"] },
    { "column": "concept", "isnull": false }
  ],
  "orderBy": ["camera_platform", "concept"]
}
```

**Distinct concepts observed below 1000 m**

```json
{
  "select": ["concept"],
  "distinct": true,
  "where": [{ "column": "depth_meters", "min": 1000 }]
}
```

**Everything observed at the same moment as all types of _Opisthoteuthidae_** — You can use [Oni](https://dsg.mbari.org/kb/v1/phylogeny/down/Opisthoteuthidae) or [FathomNet](https://database.fathomnet.org/worms/descendants/Opisthoteuthidae) to fetch all the relevant names. A shell command for fetching from MBARI's public knowledgebase might be `curl -s https://dsg.mbari.org/kb/v1/phylogeny/down/Opisthoteuthidae | jq -r '[.. | objects | .name // empty] | map("\"\(.)\"") | join(", ")'`

```json
{
  "select": ["concept", "index_recorded_timestamp", "depth_meters"],
  "where": [{ "column": "concept", "in": ["squid"] }],
  "concurrentObservations": true
}
```

**"Eating" associations only, paged**

```json
{
  "select": ["concept", "link_name", "to_concept", "link_value"],
  "where": [{ "column": "link_name", "like": "eating%" }],
  "orderBy": ["concept"],
  "limit": 1000,
  "offset": 0
}
```

## Troubleshooting

| Symptom | Likely cause |
| --- | --- |
| `select clause is required` | `download` and `run` need a non-empty `select`. |
| `where clause is required` | `count` always needs `where`; so do `download`/`run` when you pass `"strict": true`. |
| `operator does not exist: … = character varying` | A string constraint (`equals`, `in`, `like`, `contains`, `notlike`) on a numeric column. Use `min`/`max`/`minmax`. |
| `operator does not exist: character varying >= double precision` | A numeric constraint on a text column. Use `equals`/`in`/`like`. |
| A syntax error mentioning `ASC` | `orderBy` values are plain column names. `ASC` is appended for you, so `"concept DESC"` produces invalid SQL — descending order is not supported. |
| Server error on a `between` or `minmax` | Both need exactly two array elements. |
| Far more rows than expected | The view is a join; one observation can produce many rows. Narrow `select`, or set `distinct`. |
| A column you expected is missing | The view is deployment-specific. Confirm with `/v1/query/columns`. |

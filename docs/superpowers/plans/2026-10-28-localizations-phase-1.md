# Localizations Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **Brian makes all git commits himself. Never run `git add`, `git commit`, `git push` or create branches.** Each task ends with "leave uncommitted" instead of a commit step.

**Goal:** Ship first-class localizations in annosaurus as additive, unused-by-clients functionality: table, entity, DAO, controller, `/v1/localizations/*` endpoints, the `localized_annotations` query view and endpoints, `localizations` on the annotation payload, notifications, and the migration routine (created, not run).

**Architecture:** A new Java JPA entity `LocalizationEntity` hangs off `ObservationEntity` (cascade ALL, like associations) so test cleanup and deletes keep working. Pure validation lives in a Scala `LocalizationRules` object, so the controller and bulk path share one set of rules. Flyway `V1.1.0` creates the table, its Envers `_AUD` table, the quarantine table, the query view and the migration routine on both dialects. The legacy association pathway is NOT touched in Phase 1.

**Tech Stack:** Scala 3.10.0, sbt 2.0.11, Java 21 entities, Hibernate 7 / JPA (`hbm2ddl=validate`), Flyway 12, Tapir + Circe, MUnit, Testcontainers (PostgreSQL 17, SQL Server 2022).

**Spec:** `docs/proposals/2026-08-25-localizations.md` (sections 4–9, 12). DDL and routine source: `docs/proposals/localizations-migration/{postgres,sqlserver}.sql`. Client impact, for reference only: `docs/proposals/localizations-clients/`.

## Scope

**In (Phase 1, spec section 12):** everything in the Goal.
**Out (Phase 3):** rejecting legacy localization association writes with 400, excluding legacy rows from association reads and from the `annotations` view, deleting legacy rows (Phase 4). Association endpoints and the `annotations` view stay exactly as they are.
**Deliberately last:** Task 13 (mask RLE decoding). No masks exist in production (spec section 13, Q1). Tasks 1–12 accept `mask` type only for shape (encoding in the allowed list, non-empty data, positive pixel size); Task 13 adds real decoding and derived extent.

## Global Constraints

- Types: `bounding_box`, `point`, `line`, `polygon`, `mask`. Mask encodings: `coco_rle` only.
- Unique `(observation_uuid, localization_type)`; the DB enforces it. Table `localizations`; FK `observation_uuid -> observations(uuid)`.
- Coordinates normalized to `[0,1]`, top-left origin. `EPS = 1e-6`, `MIN_EXTENT = 1e-9` (same as the DB `CHECK`: `x + width <= 1.000001`, `y + height <= 1.000001`, bbox `width,height >= 1e-9`).
- `confidence` is `real`/`float(24)` in `[0,1]`. Provenance: `generator`, `verifier`, `observer`, `part` are `varchar(128)`; `comment` `varchar(1024)`; `observation_timestamp`, `verification_timestamp` are `timestamptz` / `datetimeoffset(6)`.
- Flags `training`, `truncated`, `difficult`: `boolean NOT NULL DEFAULT false` / `bit`.
- `vertices`: interleaved `[x0,y0,x1,y1,...]`, even length, every value finite and in `[0,1]`; line needs >= 2 pairs, polygon >= 3 pairs, polygon is implicitly closed (first vertex not repeated). Consecutive duplicate vertices are collapsed on write (idempotent).
- Derived extent: line/polygon/mask extents are computed by the server; clients cannot override. Point extent is exactly `0`.
- Snapping is done on EDGES (near edge, far edge, each vertex) and then `width`/`height` are derived from the snapped edges; never snap origin and extent separately (spec 6.2).
- `PUT /v1/localizations/{observation_uuid}/{localization_type}`: idempotent upsert; optional caller `last_updated_timestamp`; stale value gives **409** and no write; absent value is last-write-wins. WARN-log when an update replaces a row whose `generator` or `verifier` differs from the incoming one (spec 9).
- `POST /v1/localizations/bulk` is transactional **per batch**: any invalid item means nothing is written and the response identifies the failing item(s).
- `POST /v1/annotations` and `POST /v1/annotations/bulk` accept `localizations`, validated by the same rules and written in the same transaction; one invalid localization fails the whole request. `PUT /v1/annotations/{uuid}` does NOT carry localizations.
- `localized_annotations` view: one row per localization, no association columns, no image columns. Aliases: ancillary `x,y,z` -> `ancillary_x,ancillary_y,ancillary_z`; localization `observer` -> `localization_observer`; localization `observation_timestamp` -> `localization_observation_timestamp`.
- Flyway version `V1.1.0__localizations.sql` in BOTH `db/migrations/postgres` and `db/migrations/sqlserver`. Placeholder `${videoReferencesTable}`; defaults `video_references` (Postgres), `dbo.video_references` (SQL Server); MBARI production sets `M3_VIDEO_ASSETS.dbo.video_references`. The `it-*` suites have no vampire-squid tables and must still migrate.
- Entities are **Java** (`annosaurus/src/main/java/.../jpa/entity`). Never edit `*.scala.original`. New entities are registered only in `META-INF/persistence.xml`. `hbm2ddl=validate`: every mapped column/type must match the Flyway schema or all tests fail.
- Envers: `LocalizationEntity` is `@Audited`, so `localizations_AUD` is required in both migrations. Envers does not audit the `@Version` column, so `_AUD` has no `last_updated_timestamp`.
- Code must work on PostgreSQL AND SQL Server. Controllers return `Future[T]` and use `BaseController.exec`; no `Await` outside tests. UTC everywhere. Tests run sequentially.
- CI runs only `sbt annosaurus/test`. The `it-*` suites need Docker and are run locally: `sbt itPostgres/test`, `sbt itSqlserver/test`.

## Review Focus

Spec-implied failure modes no earlier requirement spells out. Each has a test in its owning task.

1. **Near-boundary box snapped by edge, not by origin/extent** (`y = -7.8e-7`, `height = 1.00000135` must be accepted and stored with `y = 0`, `y + height <= 1 + EPS`). Task 2.
2. **Polygon whose last two vertices repeat** (the real example `(4252,1449)` twice) is accepted and stored with the duplicate collapsed; re-submitting the stored value changes nothing. Task 2.
3. **Stale `PUT` leaves the row untouched** (409, stored values and `last_updated_timestamp` unchanged), while a `PUT` without a timestamp overwrites. Task 5, Task 6.
4. **Annotation create with one valid and one invalid localization creates no annotation at all.** Task 7.
5. **Deleting a video reference that has localizations succeeds** (FK order) and reports the count. Task 9.
6. **A second localization of the same type for one observation fails at the database** even when the controller is bypassed, with identical behavior on both dialects. Task 3.

---

## File Map

`S` = `annosaurus/src/main/scala/org/mbari/annosaurus`, `J` = `annosaurus/src/main/java/org/mbari/annosaurus/repository/jpa/entity`, `IT` = `it/src/main/scala/org/mbari/annosaurus`.

| Create | Responsibility |
|---|---|
| `S/domain/LocalizationType.scala` | the five types and encodings, parsing |
| `S/domain/Localization.scala` | `Localization`, `LocalizationSC`, `from(entity)`, `toEntity` |
| `S/domain/LocalizationRules.scala` | pure validation, snapping, derived extent, vertex normalization |
| `J/LocalizationEntity.java` | JPA entity |
| `S/repository/LocalizationDAO.scala`, `S/repository/jpa/LocalizationDAOImpl.scala` | DAO trait and JPA impl |
| `S/repository/jdbc/LocalizationSQL.scala` | raw SQL for annotation reads and video delete |
| `S/controllers/LocalizationController.scala` | upsert, delete, finds, bulk |
| `S/endpoints/LocalizationEndpoints.scala` | `/v1/localizations/*` |
| `S/etc/zeromq/...` message case | notification (Task 11) |
| `db/migrations/{postgres,sqlserver}/V1.1.0__localizations.sql` | DDL, view, routine |
| `IT/controllers/LocalizationControllerSuite.scala` and siblings | shared suites; concrete classes in `it-postgres`, `it-sqlserver` |

Modified (details in tasks): `persistence.xml`, `ObservationEntity.java`, `ImagedMomentEntity.java`, `JPADAOFactory.scala`, `Endpoints.scala` (top-level wiring), `endpoints/Endpoints.scala` (409 + schemas), `ErrorMsg.scala`, `CirceCodecs.scala`, `FlywayMigrator.scala`, `AppConfig.scala`, `reference.conf`, `EntityManagerFactories.scala`, `QueryEndpoints.scala`, `Annotation.scala`, `ImagedMomentController.scala`, `JdbcRepository.scala`, `DeleteCount.scala`, `Publisher.scala`, `NatsBridge.scala`, `NatsMessage.scala`, `TestUtils.scala`, `AssertUtils.scala`.

---

### Task 1: Domain types and codecs

**Files:**
- Create: `S/domain/LocalizationType.scala`, `S/domain/Localization.scala`
- Modify: `S/etc/circe/CirceCodecs.scala` (after the association block, ~line 134), `S/endpoints/Endpoints.scala` (Schema givens, ~48-128)
- Test: `annosaurus/src/test/scala/org/mbari/annosaurus/domain/LocalizationSuite.scala`

**Interfaces:**
- Produces:
  - `enum LocalizationType(val value: String)` with `BoundingBox("bounding_box")`, `Point("point")`, `Line("line")`, `Polygon("polygon")`, `Mask("mask")`; `object LocalizationType { def fromValue(s: String): Either[String, LocalizationType]; val values: Seq[LocalizationType] }`; `val MaskEncodings: Seq[String] = Seq("coco_rle")`.
  - `final case class Localization(localizationType: String, x: Option[Double] = None, y: Option[Double] = None, width: Option[Double] = None, height: Option[Double] = None, vertices: Option[Seq[Double]] = None, maskEncoding: Option[String] = None, maskData: Option[String] = None, maskWidthPixels: Option[Int] = None, maskHeightPixels: Option[Int] = None, generator: Option[String] = None, confidence: Option[Double] = None, observer: Option[String] = None, observationTimestamp: Option[Instant] = None, verifier: Option[String] = None, verificationTimestamp: Option[Instant] = None, comment: Option[String] = None, part: Option[String] = None, training: Boolean = false, truncated: Boolean = false, difficult: Boolean = false, uuid: Option[UUID] = None, observationUuid: Option[UUID] = None, lastUpdated: Option[Instant] = None) extends ToSnakeCase[LocalizationSC]` (x/y/width/height are `Option` because the server derives them for line/polygon/mask).
  - `LocalizationSC` is the snake_case twin (`localization_type`, `maskEncoding` -> `mask_encoding`, ..., `last_updated_time` follows `AssociationSC`) with `toCamelCase`.
  - Circe: `localizationScEncoder/Decoder`, and a `Localization` decoder accepting either casing, exactly like `associationDecoder` (`CirceCodecs.scala:129-134`). Tapir `Schema` givens `sLocalization`, `sLocalizationSc`.

- [ ] **Step 1: Write the failing test** `LocalizationSuite`: (a) `LocalizationType.fromValue("bounding_box") == Right(BoundingBox)` and `fromValue("circle")` is a `Left`; (b) a `Localization` round-trips camelCase -> `toSnakeCase` -> `toCamelCase` unchanged; (c) the JSON `{"localization_type":"point","x":0.5,"y":0.25,"verifier":"kwalz"}` decodes via `.reify[Localization]` and re-encodes without nulls (`CustomPrinter` drops them).
- [ ] **Step 2: Run** `sbt "annosaurus/testOnly org.mbari.annosaurus.domain.LocalizationSuite"`. Expected: FAIL (types undefined).
- [ ] **Step 3: Implement** the types, codecs and schemas per Interfaces, following `Association.scala` / `AssociationSC` conventions (`ToSnakeCase`, `ToCamelCase`). Entity conversion is NOT part of this task (Task 4).
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5:** Leave uncommitted.

### Task 2: Validation rules

**Files:**
- Create: `S/domain/LocalizationRules.scala`
- Test: `annosaurus/src/test/scala/org/mbari/annosaurus/domain/LocalizationRulesSuite.scala`

**Interfaces:**
- Consumes: `Localization`, `LocalizationType` (Task 1).
- Produces: `object LocalizationRules { val Eps: Double = 1e-6; val MinExtent: Double = 1e-9; def normalize(l: Localization): Either[Seq[String], Localization] }`. On success the result has `x,y,width,height` populated (server-derived for line/polygon/mask, snapped by edges), `vertices` with consecutive duplicates collapsed, and `truncated` untouched (the server never sets it; spec 6.2).

Rules, from spec 5.4 / 6.1 / 6.2: all coordinates finite and in `[0,1]` after edge snapping within `EPS`; bbox requires `width,height >= MinExtent` and `x + width <= 1`, `y + height <= 1`; point requires `x,y`, and `width`/`height` absent or `0`; line needs >= 2 pairs and polygon >= 3 pairs after duplicate collapse; `vertices` length even; `confidence` in `[0,1]`; unknown type, or a type with the wrong fields (bbox with `vertices`, line without them, mask with `vertices`), is an error. For line/polygon/mask any supplied `x,y,width,height` are ignored. Return all error messages, not only the first.

- [ ] **Step 1: Write the failing tests** in `LocalizationRulesSuite` (each `test("...")`):
  - `"bbox inside the frame is unchanged"`: `(0.1,0.2,0.3,0.4)` -> same values.
  - `"edge snapping, not origin/extent snapping"`: bbox `y = -7.8e-7, height = 1.00000135` -> `Right` with `y == 0.0` and `y + height <= 1.0 + Eps`. (Review Focus 1.)
  - `"x + width beyond 1 + eps is rejected"` and `"width below MinExtent is rejected"`.
  - `"point with nonzero width is rejected"`; `"point x = 1.0 is accepted"`.
  - `"polygon collapses consecutive duplicate vertices"` with vertices `[0.46,0.51, 0.24,0.42, 0.63,0.67, 0.89,0.48, 0.89,0.48]` -> 4 pairs; running `normalize` on the output returns an equal value (idempotent). (Review Focus 2.)
  - `"line derives extent; a horizontal line has height 0"`: `[0.1,0.5, 0.9,0.5]` -> `width == 0.8`, `height == 0.0`.
  - `"polygon with 2 pairs, odd-length vertices, NaN, and 1.2 are rejected"` (four assertions).
  - `"confidence 43.0 is rejected, 0.43 accepted"`.
  - `"client-supplied extent on a polygon is ignored"`.
- [ ] **Step 2: Run** `sbt "annosaurus/testOnly org.mbari.annosaurus.domain.LocalizationRulesSuite"`. Expected: FAIL.
- [ ] **Step 3: Implement** `normalize`. Approach: compute near/far edges per type, snap each edge onto `0`/`1` when within `Eps`, derive width/height from snapped edges, then check; vertices are snapped per value before the duplicate collapse.
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5:** Leave uncommitted.

### Task 3: Flyway migration, placeholder, config

**Files:**
- Create: `annosaurus/src/main/resources/db/migrations/postgres/V1.1.0__localizations.sql`, `.../sqlserver/V1.1.0__localizations.sql`
- Modify: `S/etc/flyway/FlywayMigrator.scala`, `S/AppConfig.scala` (`DatabaseConfig`, ~96), `annosaurus/src/main/resources/reference.conf` (`database {}`), `S/repository/jpa/EntityManagerFactories.scala:72-94`
- Test: `IT/repository/jpa/LocalizationConstraintsSuite.scala` (shared trait); concrete `PostgresLocalizationConstraintsSuite` in `it-postgres/src/test/scala/org/mbari/annosaurus/repository/jpa/`, `SqlServerLocalizationConstraintsSuite` in `it-sqlserver/...`

**Interfaces:**
- Produces: `DatabaseConfig(url, user, password, driver, queryView, videoReferencesTable: Option[String] = None)` (new LAST field with a default, so positional call sites in `EntityManagerFactories`, `PostgresTestDAOFactory`, `SqlServerTestDAOFactory`, `TestDAOFactory` keep compiling); config key `database.video.references.table` with env `DATABASE_VIDEO_REFERENCES_TABLE` (default unset in `reference.conf`); `FlywayMigrator.migrate` passes `.placeholders(Map("videoReferencesTable" -> value).asJava)` where `value = config.videoReferencesTable.getOrElse(default)` and the default is `video_references` for Postgres, `dbo.video_references` for SQL Server (chosen from `Databases.typeFromUrl`).

- [ ] **Step 1: Write the failing test** `LocalizationConstraintsSuite extends BaseDAOSuite`. Using the factory's `entityManagerFactory`/JDBC, insert an observation (via `TestUtils.create(1,1,0)`), then raw-SQL insert into `localizations` and assert (each a test): a valid bbox row inserts; a second `bounding_box` row for the same observation fails (Review Focus 6); a `point` with `width = 0.1` fails; a `bounding_box` with `vertices` set fails; a `polygon` without `vertices` fails; `confidence = 43` fails; `x + width = 1.5` fails; `localization_type = 'circle'` fails; a `point` and a `bounding_box` on the same observation both succeed; `difficult` defaults to false. Also a test that `localized_annotations` returns one row for the bbox with `ancillary_x` present as a column name. Make the assertion helper accept any `SQLException` so it is identical on both dialects.
- [ ] **Step 2: Run** `sbt "itPostgres/testOnly org.mbari.annosaurus.repository.jpa.PostgresLocalizationConstraintsSuite"`. Expected: FAIL (no table).
- [ ] **Step 3: Implement.** Build each `V1.1.0` file from `docs/proposals/localizations-migration/<dialect>.sql` verbatim (table, indexes, rejects table, `localized_annotations` view, helper functions, `migrate_localizations` routine, SQL Server synonym) and ADD: (a) `localizations_AUD` with the audited columns (every `localizations` column except `last_updated_timestamp`, plus `REV` and `REVTYPE`, primary key `(REV, uuid)`, same types as `associations_AUD` in the same dialect, FK to `REVINFO` named like `fk_localizations_aud__revinfo`); (b) nothing else. Then implement `DatabaseConfig`, `AppConfig`, `reference.conf`, `EntityManagerFactories` and `FlywayMigrator` per Interfaces.
- [ ] **Step 4: Run** the Postgres suite, then `sbt "itSqlserver/testOnly org.mbari.annosaurus.repository.jpa.SqlServerLocalizationConstraintsSuite"`, then `sbt itPostgres/test itSqlserver/test` to prove the existing suites still migrate with no vampire-squid tables. Expected: all PASS. (`hbm2ddl=validate` will not exercise the new table until Task 4 maps it.)
- [ ] **Step 5:** Leave uncommitted.

### Task 4: Entity, DAO, factory

**Files:**
- Create: `J/LocalizationEntity.java`, `S/repository/LocalizationDAO.scala`, `S/repository/jpa/LocalizationDAOImpl.scala`
- Modify: `annosaurus/src/main/resources/META-INF/persistence.xml` (add `<class>org.mbari.annosaurus.repository.jpa.entity.LocalizationEntity</class>` after line 18), `J/ObservationEntity.java` (~183-299), `J/ImagedMomentEntity.java` (`initializeLazyRelations`), `S/repository/jpa/JPADAOFactory.scala:34-78`, `IT/controllers/TestUtils.scala`, `IT/AssertUtils.scala`
- Test: `IT/repository/jpa/LocalizationDAOSuite.scala`; concrete `PostgresLocalizationDAOSuite`, `SqlServerLocalizationDAOSuite`

**Interfaces:**
- Produces:
  - `LocalizationEntity implements IPersistentObject`, `@Entity(name="Localization") @Table(name="localizations")`, `@EntityListeners({TransactionLogger.class, TransactionNotifier.class})`, `@org.hibernate.envers.Audited`; id/`@Version` exactly as `AssociationEntity.java:113-122`; `@ManyToOne(cascade={PERSIST,DETACH}, optional=false) @JoinColumn(name="observation_uuid", nullable=false, foreignKey=@ForeignKey(name="fk_localizations__observation_uuid")) ObservationEntity observation`; getters/setters for every column; `Double`/`Integer`/`Float` for nullable numerics, `boolean` for flags, `Instant` for the two timestamps (the `InstantConverter` applies). Named queries `Localization.findAll`, `Localization.findByObservationUuid`, `Localization.findByObservationUuidAndType`.
  - `object Localization { def from(entity: LocalizationEntity): Localization }` (always fills `observationUuid`; `vertices` parsed from the entity's JSON text `[x0,y0,...]`) and `Localization#toEntity: LocalizationEntity` (writes `vertices` as that JSON text), in `S/domain/Localization.scala`.
  - `ObservationEntity`: `@OneToMany(targetEntity=LocalizationEntity.class, cascade=ALL, fetch=LAZY, mappedBy="observation", orphanRemoval=true) Set<LocalizationEntity> localizations`, plus `getLocalizations`, `setLocalizations`, `addLocalization(l)` (sets `l.setObservation(this)`), `removeLocalization(l)`.
  - `trait LocalizationDAO[T <: IPersistentObject] extends DAO[T] { def findByObservationUuid(uuid: UUID): Seq[T]; def findByObservationUuidAndType(uuid: UUID, localizationType: String): Option[T]; def findByVideoReferenceUuid(uuid: UUID, limit: Option[Int], offset: Option[Int]): Seq[T] }` and `class LocalizationDAOImpl(entityManager: EntityManager) extends BaseDAO[LocalizationEntity](entityManager) with LocalizationDAO[LocalizationEntity]`; `JPADAOFactory` gains `newLocalizationDAO(): LocalizationDAOImpl` and `newLocalizationDAO(dao: DAO[?]): LocalizationDAOImpl`.
  - `TestUtils.randomLocalization(localizationType: String = "bounding_box"): LocalizationEntity` (valid values per type) and `TestUtils.create(nImagedMoments, nObservations, nAssociations, nImageReferences, includeData, nLocalizations: Int = 0)` (`nLocalizations` is a new LAST parameter with a default; the 4th parameter is the existing `nImageReferences`, so callers MUST pass it by name: `TestUtils.create(1, 1, nLocalizations = 2)`); `AssertUtils.assertSameLocalization(a: LocalizationEntity, b: LocalizationEntity)`.

- [ ] **Step 1: Write the failing test** `LocalizationDAOSuite extends BaseDAOSuite` (pattern: `AssociationDAOSuite`): create an observation with `TestUtils.create(1, 1, nLocalizations = 2)` (a box and a point); assert `findByObservationUuid` returns 2; `findByObservationUuidAndType(uuid, "point")` returns the point; `findByVideoReferenceUuid` returns both; `deleteByUUID` removes one; deleting the imaged moment via `daoFactory.cleanup()` removes localizations with it (cascade). Also add to `LocalizationSuite` (annosaurus unit tests, Task 1's file) a test that `vertices` `Some(Seq(0.1,0.2,0.3,0.4))` survives `toEntity` then `from`, and that every field of a fully populated `Localization` does.
- [ ] **Step 2: Run** `sbt "itPostgres/testOnly org.mbari.annosaurus.repository.jpa.PostgresLocalizationDAOSuite"`. Expected: FAIL (compile).
- [ ] **Step 3: Implement** per Interfaces. Native queries bind UUIDs through `setUuidParameter` (lowercase strings) as `BaseDAO` does. Implement `Localization.from`/`toEntity`.
- [ ] **Step 4: Run** both concrete DAO suites and `sbt annosaurus/test`. Expected: PASS, and no `validate` failure on startup on either dialect.
- [ ] **Step 5:** Leave uncommitted.

### Task 5: Controller and 409

**Files:**
- Create: `S/controllers/LocalizationController.scala`, `IT/controllers/LocalizationControllerSuite.scala` (+ concrete Postgres/SqlServer classes)
- Modify: `S/domain/ErrorMsg.scala`, `S/etc/circe/CirceCodecs.scala` (ErrorMsg codecs, ~51-70)

**Interfaces:**
- Consumes: `LocalizationRules.normalize` (Task 2), `JPADAOFactory.newLocalizationDAO` (Task 4).
- Produces:
  - `final case class Conflict(message: String, current: Option[LocalizationSC] = None, responseCode: Int = 409) extends ErrorMsg` and `final class StaleLocalizationException(val current: Localization) extends RuntimeException`.
  - `class LocalizationController(val daoFactory: JPADAOFactory, bus: Subject[Any] = EventBus.RxSubject) extends BaseController[LocalizationEntity, LocalizationDAO[LocalizationEntity], Localization]` with:
    - `upsert(observationUuid: UUID, localizationType: String, localization: Localization)(implicit ec: ExecutionContext): Future[Localization]` (validates with `LocalizationRules`, loads the observation via `daoFactory.newObservationDAO(dao)`, `NotFoundInDatastoreException` if missing; no existing row -> insert; existing row -> if `localization.lastUpdated` is defined and differs from the stored `last_updated_timestamp` fail with `StaleLocalizationException(current)` and write nothing, otherwise update in place keeping the uuid; WARN when `generator` or `verifier` changes; publish after commit).
    - `delete(observationUuid: UUID, localizationType: String)(implicit ec): Future[Boolean]`
    - `findByObservationUuid(uuid: UUID)(implicit ec): Future[Seq[Localization]]`
    - `findByVideoReferenceUuid(uuid: UUID, limit: Option[Int], offset: Option[Int])(implicit ec): Future[Seq[Localization]]`
    - `bulkUpsert(items: Seq[Localization])(implicit ec): Future[Seq[Localization]]` (each item needs `observationUuid`; one transaction; the first invalid or stale item aborts the batch; failures raise `BulkLocalizationException(failures: Seq[(Int, Seq[String])])`).
    - plus inherited `findByUUID`.

- [ ] **Step 1: Write the failing tests** `LocalizationControllerSuite` (pattern `AssociationControllerSuite`): `upsert` a bbox creates it and returns a `uuid` and `lastUpdated`; a second `upsert` for the same key returns the SAME uuid (idempotent); an `upsert` with a stale `lastUpdated` fails with `StaleLocalizationException` and `findByObservationUuid` still returns the original values and timestamp (Review Focus 3); an `upsert` without `lastUpdated` overwrites; an invalid bbox fails with a validation error and writes nothing; `upsert` for an unknown observation fails with `NotFoundInDatastoreException`; `delete` returns true then false; `bulkUpsert` of three valid items writes three, of two valid plus one invalid writes none and reports index 2.
- [ ] **Step 2: Run** `sbt "itPostgres/testOnly org.mbari.annosaurus.controllers.PostgresLocalizationControllerSuite"`. Expected: FAIL.
- [ ] **Step 3: Implement** per Interfaces; add the `Conflict` case and its Circe codecs (`conflictEncoder`/`conflictDecoder`).
- [ ] **Step 4: Run** both concrete suites. Expected: PASS.
- [ ] **Step 5:** Leave uncommitted.

### Task 6: Endpoints

**Files:**
- Create: `S/endpoints/LocalizationEndpoints.scala`, `IT/endpoints/LocalizationEndpointsSuite.scala` (+ concrete classes)
- Modify: `S/endpoints/Endpoints.scala` (add a `Conflict` `oneOfVariant` with `StatusCode.Conflict` to BOTH `secureEndpoint` and `openEndpoint`, and `Schema` givens for `Conflict`), `S/Endpoints.scala` (top-level: controller val ~38-50, endpoint val ~57-74, `blockingEndpoints` ~82-96)

**Interfaces:**
- Produces: `class LocalizationEndpoints(controller: LocalizationController)(using ec: ExecutionContext, jwtService: JwtService) extends Endpoints` (pattern `AssociationEndpoints.scala:42`; `base = "localizations"`, `tag = "Localizations"`) with an endpoint/impl pair for each of the nine routes in spec 7.2, excluding the three `query/*` ones (Task 10): `GET {uuid}`, `GET observation/{observationUuid}`, `GET videoreference/{videoReferenceUuid}` (with `paging`), `PUT {observationUuid}/{localizationType}` (secure), `DELETE {observationUuid}/{localizationType}` (secure), `POST bulk` (secure), `GET list/types`, `GET list/maskencodings`, `GET list/constraints`. Error mapping: validation failure -> `BadRequest`; `StaleLocalizationException` -> `Conflict("...", Some(current.toSnakeCase))`; `BulkLocalizationException` -> `BadRequest` naming each failing index; reads are open, writes secure. Both `all` and `allImpl` list every endpoint.

- [ ] **Step 1: Write the failing tests** `LocalizationEndpointsSuite` (pattern `AssociationEndpointsSuite`, JWT from `JwtService("mbari","foo","bar")`): `PUT` a bbox -> 200 and body has `uuid`; `GET /v1/localizations/observation/{uuid}` -> 1 element; `PUT` with stale `last_updated_time` -> 409 whose body has `current` and the stored row is unchanged (Review Focus 3); `PUT` without a token -> 401; invalid bbox -> 400; `DELETE` -> 200 then `GET {uuid}` -> 404; `POST bulk` with one invalid item -> 400 and `GET observation/...` shows nothing was written; `GET list/types` returns the five values in spec order; `GET list/constraints` returns both lists.
- [ ] **Step 2: Run** `sbt "itPostgres/testOnly org.mbari.annosaurus.endpoints.PostgresLocalizationEndpointsSuite"`. Expected: FAIL.
- [ ] **Step 3: Implement** per Interfaces.
- [ ] **Step 4: Run** both concrete suites, then `sbt annosaurus/compile`. Expected: PASS.
- [ ] **Step 5:** Leave uncommitted.

### Task 7: Annotation create carries localizations

**Files:**
- Modify: `S/domain/Annotation.scala` (`Annotation`, `AnnotationSC`, `BulkAnnotationSC`, `toEntity`, `from`, `removeForeignKeys`, `toSnakeCase`, `toCamelCase`, ~24-80), `S/controllers/ImagedMomentController.scala` (re-attach loops at ~335-345 in `create` and ~471-481 in `bulkCreate`), `S/etc/circe/CirceCodecs.scala` if derivation needs the new givens in order
- Test: `IT/controllers/AnnotationControllerSuite.scala` (extend; concrete classes already exist)

**Interfaces:**
- Produces: `Annotation.localizations: Seq[Localization] = Nil`; `AnnotationSC.localizations: Seq[LocalizationSC] = Nil`; `BulkAnnotationSC.localizations: Option[Seq[LocalizationSC]]`; on create each localization is run through `LocalizationRules.normalize` BEFORE any entity is persisted and attached with `observation.addLocalization`, uuids nulled like associations; `AnnotationController.create/bulkCreate` signatures unchanged. `PUT /v1/annotations/{uuid}` is unchanged.

- [ ] **Step 1: Write the failing tests** (add to `AnnotationControllerSuite`): `create` of an annotation with a bbox and a point returns an annotation whose `localizations` has two entries with `uuid`s, and `LocalizationController.findByObservationUuid` returns both; `bulkCreate` of three annotations, one carrying a polygon, persists it; `create` of an annotation with one valid and one invalid localization (an out-of-bounds bbox) fails AND no annotation exists afterwards (assert via `findByVideoReferenceUuid` returning empty) (Review Focus 4); `create` with no localizations behaves exactly as before (existing tests stay green).
- [ ] **Step 2: Run** `sbt "itPostgres/testOnly org.mbari.annosaurus.controllers.PostgresAnnotationControllerSuite"`. Expected: FAIL.
- [ ] **Step 3: Implement** per Interfaces. Validation failure surfaces as `BadRequest` through the existing annotation endpoint error handling, which is the existing "invalid association" path.
- [ ] **Step 4: Run** both concrete suites and `sbt annosaurus/test` (codec and domain suites).
- [ ] **Step 5:** Leave uncommitted.

### Task 8: Annotation reads return localizations

**Files:**
- Create: `S/repository/jdbc/LocalizationSQL.scala`
- Modify: `S/domain/Annotation.scala` (`Annotation.from`), `S/repository/jdbc/JdbcRepository.scala` (`executeQueryForAnnotations` ~441, `executeQuery` ~483), `S/domain/Observation.scala` only if it also exposes children
- Test: `IT/endpoints/AnnotationEndpointsSuite.scala`, `IT/endpoints/FastAnnotationEndpointsSuite.scala` (extend; concrete classes exist), or the nearest existing read suites if names differ

**Interfaces:**
- Produces: `object LocalizationSQL` mirroring `AssociationSQL` (`SELECT`, `FROM`, `ORDER`, `byObservationUuids`, `byVideoReferenceUuid`, `resultListToLocalizations(rows: List[?]): Seq[Localization]`, `join(annotations, localizations)`); the JPA read path fills `Annotation.localizations` from `entity.getLocalizations` inside the transaction; the JDBC path batches observation uuids in groups of 200 like associations. Association behavior is unchanged.

- [ ] **Step 1: Write the failing tests:** create an annotation with a bbox, then `GET /v1/annotations/{observationUuid}`, `GET /v1/annotations/videoreference/{uuid}`, `GET /v1/annotations/imagereference/{uuid}` (create an image reference on the moment first), and one `fast` endpoint each return it with `localizations` of length 1 and matching values; an annotation with associations only returns `localizations == []`.
- [ ] **Step 2: Run** the concrete Postgres suites for those endpoints. Expected: FAIL.
- [ ] **Step 3: Implement** per Interfaces.
- [ ] **Step 4: Run** the Postgres and SQL Server suites for the touched endpoints. Expected: PASS.
- [ ] **Step 5:** Leave uncommitted.

### Task 9: Video delete removes localizations

**Files:**
- Modify: `S/repository/jdbc/JdbcRepository.scala` (`deleteByVideoReferenceUuid` ~59-97), `S/repository/jdbc/LocalizationSQL.scala` (add `deleteByVideoReferenceUuid`), `S/domain/DeleteCount.scala` (+ `DeleteCountSC`)
- Test: the existing suite covering `deleteByVideoReferenceUuid` (find it with `grep -rn deleteByVideoReferenceUuid it/`), extended

**Interfaces:**
- Produces: `DeleteCount` and `DeleteCountSC` gain `localizationCount: Int` (`localization_count`) inserted **before** `observationCount`, positional reads in `JdbcRepository` updated to match; `LocalizationSQL.deleteByVideoReferenceUuid` runs BEFORE the observation delete.

- [ ] **Step 1: Write the failing test:** create an annotation with a bbox and a point under a video reference, call the delete, assert it succeeds (no FK violation), the returned `localizationCount == 2`, and `findByVideoReferenceUuid` is empty (Review Focus 5). Check every existing assertion on `DeleteCount` still compiles.
- [ ] **Step 2: Run** the Postgres suite. Expected: FAIL (FK violation or compile).
- [ ] **Step 3: Implement** per Interfaces; keep FK order `ancillary, image refs, associations, localizations, observations, imaged moments`.
- [ ] **Step 4: Run** both dialects.
- [ ] **Step 5:** Leave uncommitted.

### Task 10: Localization query endpoints

**Files:**
- Modify: `S/endpoints/QueryEndpoints.scala`, `S/Endpoints.scala` (second `QueryController`/`QueryEndpoints`), `S/AppConfig.scala`, `reference.conf`
- Create: `IT/endpoints/LocalizationQueryEndpointsSuite.scala` (+ concrete classes)

**Interfaces:**
- Produces: `QueryEndpoints(queryController: QueryController, base: EndpointInput[Unit] = "query", namePrefix: String = "", tag: String = "Query")` where every `.name(...)` becomes `namePrefix + "runQuery"` etc. and every path uses `base`; config key `database.query.localization.view = "localized_annotations"` (env `DATABASE_QUERY_LOCALIZATION_VIEW`); `AppConfig.LocalizationQueryView`; a second pair in the top-level wiring: `new QueryController(AppConfig.DefaultDatabaseConfig, AppConfig.LocalizationQueryView)` and `new QueryEndpoints(localizationQueryController, "localizations" / "query", "localization", "Localization Query")`. The four routes are `GET .../columns`, `POST .../run`, `POST .../download`, `POST .../count` (returns `Count`, body `{"count": <n>}`). Existing `/v1/query/*` unchanged.

- [ ] **Step 1: Write the failing tests:** seed two annotations with a bbox, a polygon and a point; `POST /v1/localizations/query/run` with `where: [{column: "localization_type", equals: "bounding_box"}]` returns one row and has `localization_uuid`, `part`, `ancillary_x`; a `confidence` range filter and a `verifier` `like` filter work; `count` returns `{"count": 1}` for the same request; `columns` lists `localization_uuid` and no `link_name`/`image_url`; the existing `/v1/query/columns` is unchanged. The view's `concept` filter matches the observation's concept only (a localization with `part = "X"` is not found by `concept = "X"`).
- [ ] **Step 2: Run** the Postgres suite. Expected: FAIL.
- [ ] **Step 3: Implement.** If `PreparedStatementGenerator` hard-codes columns that the new view lacks (`IndexTime`, `ObservationUuid`, `ImagedMomentUuid`), make the tests run with `strict = true` and say so in the endpoint description; do not change the existing view's behavior.
- [ ] **Step 4: Run** both dialects plus `sbt annosaurus/test`.
- [ ] **Step 5:** Leave uncommitted.

### Task 11: Notifications

**Files:**
- Modify: `S/messaging/Publisher.scala` (`created`, ~42), `S/etc/zeromq/ZeroMQMessage.scala` (new `LocalizationCreatedMessage(content: Localization)` modeled on `AssociationCreatedMessage`), `S/etc/nats/NatsBridge.scala:35-60`, `S/etc/nats/NatsMessage.scala` (`enum DataTypes` gains `LOCALIZATION`), `S/controllers/LocalizationController.scala` (publish)
- Test: `annosaurus/src/test/scala/org/mbari/annosaurus/messaging/PublisherSuite.scala` (create if absent) and an `it` assertion in `LocalizationControllerSuite`

**Interfaces:** `Publisher.created(x: Any)` handles `Localization`; `NatsBridge` maps `LocalizationEntity` to `DataTypes.LOCALIZATION`; no association notification is ever emitted for a localization.

- [ ] **Step 1: Write the failing tests:** `Publisher.created(localization)` emits a `LocalizationCreatedMessage` (subscribe to the bus used in the existing publisher tests; if none exist, test with an `EventBus.RxSubject` test subscriber); in the controller suite, an `upsert` publishes exactly one localization message and zero association messages; `NatsBridge` returns `Some(LOCALIZATION)` for the entity class (unit-level, no broker).
- [ ] **Step 2: Run** `sbt annosaurus/test` and the controller suite. Expected: FAIL.
- [ ] **Step 3: Implement** per Interfaces.
- [ ] **Step 4: Run** the same commands. Expected: PASS.
- [ ] **Step 5:** Leave uncommitted.

### Task 12: Migration routine integration test

**Files:**
- Create: `IT/repository/jpa/LocalizationMigrationSuite.scala` (+ concrete Postgres/SqlServer classes)

**Interfaces:** Consumes the `V1.1.0` routine from Task 3. In `beforeAll` the suite creates the vampire-squid stand-in (`video_references(uuid, width, height)`; `dbo.video_references` on SQL Server, which the synonym resolves) and drops it in `afterAll`.

- [ ] **Step 1: Write the failing tests.** Port the scratch harness used while drafting the scripts (rows and expectations are in `docs/proposals/2026-08-25-localizations.md` section 8.2, "Status"): seed the 25 legacy associations (one 0–100 video incl. a value `0.5`, one 0–1 video, a video with no confidences, a video whose only value is `150`, a video with values on both sides of `1`, an orphaned and a sizeless image reference, a missing video row, every reject reason, duplicate-vertex polygon, edge-clamped box, `to_concept` values `eye`, `self`, `Self`). Assert: dry run (`BEGIN; ...; ROLLBACK`) leaves zero rows; real run yields 15 migrated (1 `truncated`) and 10 rejects, one per reason (`malformed_json` twice); `0.43` for confidence `43` and `0.005` for `0.5` in the 0–100 video; `0.87` unchanged in the 0–1 video; `part` is `NULL` for `self` and kept for `eye` and `Self`; a second run migrates 0; after inserting a new legacy row with confidence `0.3` into the already-migrated 0–100 video, a third run stores `0.003`; the polygon has 4 pairs.
- [ ] **Step 2: Run** `sbt "itPostgres/testOnly org.mbari.annosaurus.repository.jpa.PostgresLocalizationMigrationSuite"`. Expected: FAIL only if the routine or seed differs from the drafted behavior; if it passes immediately, the Task 3 migration already satisfies it, which is acceptable for a characterization test.
- [ ] **Step 3: Fix** any divergence in the `V1.1.0` files (not the proposal scripts, unless the proposal is wrong; if so, say so in your report).
- [ ] **Step 4: Run** both dialects.
- [ ] **Step 5:** Leave uncommitted.

### Task 13: Mask RLE decoding

**Files:**
- Create: `S/domain/CocoRle.scala`, `annosaurus/src/test/scala/org/mbari/annosaurus/domain/CocoRleSuite.scala`
- Modify: `S/domain/LocalizationRules.scala`

**Interfaces:**
- Produces: `object CocoRle { def decodeCounts(compressed: String): Either[String, Vector[Long]]; def extent(counts: Vector[Long], height: Int, width: Int): Either[String, (Int, Int, Int, Int)] }` returning the pixel bounding box `(minX, minY, maxXExclusive, maxYExclusive)` of the set pixels, using COCO's column-major order; `LocalizationRules.normalize` for `mask` requires `maskEncoding == "coco_rle"`, `maskWidthPixels`/`maskHeightPixels` positive, `mask_data` decodable, the decoded run lengths summing to `width * height`, and sets `x,y,width,height` to the pixel box divided by `maskWidthPixels` / `maskHeightPixels`; an all-background mask is rejected.

- [ ] **Step 1: Write the failing tests** with vectors generated by pycocotools, not hand-written: for a 6x4 mask with one set rectangle, a mask touching the corner, and an empty mask, generate `counts` with `pycocotools.mask.encode` (`pip install pycocotools` in a scratch virtualenv) and embed the strings and expected boxes as constants with a comment giving the generating call. Assert `decodeCounts` then `extent`; truncated and non-ASCII strings are `Left`; `normalize` on a valid mask yields extent `pixelBox / (width,height)`; wrong total size is rejected.
- [ ] **Step 2: Run** `sbt "annosaurus/testOnly org.mbari.annosaurus.domain.CocoRleSuite"`. Expected: FAIL.
- [ ] **Step 3: Implement** the pycocotools compressed-counts decoding (each character carries 5 data bits plus a continuation bit, signed, with run `i > 2` stored as a delta from run `i - 2`) per the pycocotools `rleFrString` reference; do not invent a different scheme.
- [ ] **Step 4: Run** the suite, then `sbt annosaurus/test` and, for the endpoint-level mask test (`PUT` a mask returns derived extent), `sbt itPostgres/test`. Expected: PASS.
- [ ] **Step 5:** Leave uncommitted.

### Task 14: Full verification

- [ ] **Step 1:** `sbt annosaurus/test` — all pass.
- [ ] **Step 2:** `sbt itPostgres/test` — all pass.
- [ ] **Step 3:** `sbt itSqlserver/test` — all pass.
- [ ] **Step 4:** `sbt stage` — succeeds, and `annosaurus/target/universal/stage/` is populated.
- [ ] **Step 5:** Report which files changed with `git status` (read-only). Do not commit.

---

## Self-review

- **Spec coverage:** section 5 schema, `_AUD` and view -> Task 3/4; 5.4 type matrix and 6.1/6.2 contracts -> Task 2 (+13 for masks); 6.3 provenance/flags -> Tasks 1/3/4; 7.2 endpoints -> Task 6, query routes -> Task 10, annotation create -> Task 7, annotation reads incl. `imagereference` -> Task 8; 8.2 routine -> Tasks 3/12; 9 audit and messaging -> Tasks 4/11 and the WARN in Task 5; 10 testing table -> Tasks 2, 3, 5, 6, 7, 12 (the "companion code" differential test is NOT included: it needs the reference Java from `bounding-box-coordinates.md` and belongs with that document); 11/12 limitations and rollout -> Scope. Phase 3 items are excluded by design.
- **Calls this plan makes that the spec leaves open** (change before implementation if you disagree): REST `vertices` is a JSON number array and the DB stores it as JSON text; `Localization` x/y/width/height are optional because the server derives them for non-box types; 409 body is `Conflict(message, current)`; bulk failure is a 400 naming failing indices; `localizations_AUD` is added so the entity can be `@Audited` as spec section 9 says; the second query view is wired through a parameterized `QueryEndpoints`; mask decoding is split into Task 13 because no masks exist in production.
- **Known unknowns for the implementer to resolve by reading code, not guessing:** whether `PreparedStatementGenerator` works non-strict against the new view (Task 10); which existing suite covers `deleteByVideoReferenceUuid` (Task 9); whether `ImagedMomentEntity.initializeLazyRelations` must also touch `localizations` (Task 4).
- **Type consistency:** `Localization`/`LocalizationSC`, `LocalizationRules.normalize`, `LocalizationController.upsert/bulkUpsert`, `StaleLocalizationException`, `BulkLocalizationException`, `Conflict`, and `DatabaseConfig.videoReferencesTable` are defined once and referenced by name afterwards.

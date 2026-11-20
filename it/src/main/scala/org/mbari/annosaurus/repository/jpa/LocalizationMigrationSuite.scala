/*
 * Copyright 2017 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.annosaurus.repository.jpa

import java.sql.Connection
import java.util.UUID
import scala.collection.mutable
import scala.util.Using

/**
 * Characterization test of the database routine that converts legacy localization associations into
 * `localizations` (V1.1.0 migration, proposal section 8.2): `migrate_localizations()` on PostgreSQL,
 * `dbo.migrate_localizations` on SQL Server. Dialect-specific SQL is isolated in the abstract members.
 *
 * The it-* databases have no vampire-squid `video_references` table, so the suite creates a stand-in in
 * `beforeAll` and drops it (and every legacy row it seeded) afterwards.
 */
trait LocalizationMigrationSuite extends BaseDAOSuite:

    // ---- dialect hooks ------------------------------------------------------------------------

    /** CREATE TABLE statement(s) for the stand-in video_references(uuid, width, height). */
    def createVideoReferencesTable: Seq[String]

    def dropVideoReferencesTable: String

    /** SQL that runs the routine and returns rows (outcome, n). */
    def routineSql: String

    /** Name of the stand-in table (qualified as needed) for inserting rows. */
    def videoReferencesName: String

    // ---- helpers ------------------------------------------------------------------------------

    private val Ts = "'2026-01-01 00:00:00'"


    private def vid(c: Char): UUID = UUID.fromString(s"${c}0000000-0000-0000-0000-00000000000$c")
    private def im(tag: String)    = UUID.nameUUIDFromBytes(s"im:$tag".getBytes)
    private def ob(tag: String)    = UUID.nameUUIDFromBytes(s"ob:$tag".getBytes)

    private def sqlStr(s: String): String = if s == null then "NULL" else "'" + s.replace("'", "''") + "'"

    private def withConn[A](f: Connection => A): A =
        Using.resource(daoFactory.databaseConfig.newConnection())(f)

    private def execute(sql: String): Unit =
        withConn(c => Using.resource(c.createStatement())(_.executeUpdate(sql)))

    private def scalar(sql: String): Long =
        withConn(c =>
            Using.resource(c.createStatement()) { s =>
                val rs = s.executeQuery(sql)
                rs.next()
                rs.getLong(1)
            }
        )

    private def seed(
        tag: String,
        video: UUID,
        linkName: String,
        linkValue: String,
        toConcept: String = null,
        ts: String = Ts
    ): Unit =
        // imaged moment and observation are shared by tags that repeat (b_dup)
        withConn { c =>
            Using.resource(c.createStatement()) { s =>
                def exists(table: String, id: UUID): Boolean =
                    val rs = s.executeQuery(s"SELECT 1 FROM $table WHERE uuid = '$id'")
                    try rs.next()
                    finally rs.close()
                if !exists("imaged_moments", im(tag)) then
                    s.executeUpdate(
                        s"INSERT INTO imaged_moments(uuid, video_reference_uuid, last_updated_timestamp) VALUES ('${im(tag)}', '$video', $Ts)"
                    )
                if !exists("observations", ob(tag)) then
                    s.executeUpdate(
                        s"INSERT INTO observations(uuid, imaged_moment_uuid, concept, observation_timestamp, last_updated_timestamp) " +
                            s"VALUES ('${ob(tag)}', '${im(tag)}', '$tag', $Ts, $Ts)"
                    )
                s.executeUpdate(
                    s"INSERT INTO associations(uuid, observation_uuid, link_name, link_value, to_concept, mime_type, last_updated_timestamp) " +
                        s"VALUES ('${UUID.randomUUID()}', '${ob(tag)}', ${sqlStr(linkName)}, ${sqlStr(linkValue)}, ${sqlStr(
                                toConcept
                            )}, 'application/json', $ts)"
                )
            }
        }

    private val BB  = "bounding box"
    private val Pt  = "localization-point"
    private val Ln  = "localization-line"
    private val Pg  = "localization-polygon"
    private val VA  = vid('a')
    private val VB  = vid('b')
    private val VC  = vid('c')
    private val VD  = vid('d')
    private val VE  = vid('e') // deliberately has NO video_references row
    private val VF  = vid('f')

    /** The 25 legacy associations plus b_case, b_closed and b_aba; see the proposal, section 8.2. */
    private def seedLegacy(): Unit =
        execute(
            s"""INSERT INTO $videoReferencesName(uuid, width, height) VALUES
               | ('$VA', 4800, 3000), ('$VB', 1000, 500), ('$VC', 1000, 500), ('$VD', 1000, 500), ('$VF', 1000, 500)""".stripMargin
        )
        // V_A 4800x3000, a 0-100 pipeline
        seed("a_bbox1", VA, BB, """{"x":960,"y":300,"width":480,"height":600,"generator":"mbari452k_yolov8","confidence":43.0}""", "eye")
        seed("a_bbox2", VA, BB, """{"x":0,"y":0,"width":240,"height":300,"generator":"mbari452k_yolov8","confidence":17.0,"verifier":"kwalz","tags":["training","other"]}""")
        seed("a_low", VA, BB, """{"x":0,"y":0,"width":480,"height":300,"confidence":0.5}""", "self")
        seed("a_point", VA, Pt, """{"x":[2400],"y":[1500]}""")
        seed("a_line", VA, Ln, """{"x":[1630,4381],"y":[2312,1645]}""")
        seed("a_poly", VA, Pg, """{"x":[2208,1167,3000,4252,4252],"y":[1525,1263,2000,1449,1449]}""")
        // V_B 1000x500, a 0-1 pipeline
        seed("b_edge", VB, BB, """{"x":950,"y":0,"width":100,"height":100,"confidence":0.87}""")
        seed("b_out", VB, BB, """{"x":2000,"y":10,"width":50,"height":50}""")
        seed("b_dup", VB, BB, """{"x":0,"y":0,"width":100,"height":100,"confidence":0.1}""")
        seed("b_dup", VB, BB, """{"x":0,"y":0,"width":200,"height":100,"confidence":0.2}""", ts = "'2026-02-01 00:00:00'")
        seed("b_orphan", VB, BB, """{"x":100,"y":50,"width":200,"height":100,"image_reference_uuid":"99999999-9999-9999-9999-999999999999"}""")
        seed("b_img_ok", VB, BB, """{"x":200,"y":100,"width":400,"height":200,"image_reference_uuid":"11111111-1111-1111-1111-111111111111"}""")
        seed("b_img_nosize", VB, BB, """{"x":100,"y":50,"width":200,"height":100,"image_reference_uuid":"22222222-2222-2222-2222-222222222222"}""")
        execute(
            s"""INSERT INTO image_references(uuid, imaged_moment_uuid, height_pixels, width_pixels, url, last_updated_timestamp) VALUES
               | ('11111111-1111-1111-1111-111111111111', '${im("b_img_ok")}', 1000, 2000, 'http://x/ok.png', $Ts),
               | ('22222222-2222-2222-2222-222222222222', '${im("b_img_nosize")}', NULL, NULL, 'http://x/nosize.png', $Ts)""".stripMargin
        )
        seed("b_bad_conf", VB, BB, """{"x":0,"y":0,"width":10,"height":10,"confidence":"abc"}""")
        seed("b_zero", VB, BB, """{"x":0,"y":0,"width":0,"height":10}""")
        seed("b_mismatch", VB, Pg, """{"x":[1,2,3],"y":[1,2]}""")
        seed("b_few", VB, Pg, """{"x":[1,2],"y":[1,2]}""")
        // explicitly closed polygon [A,B,C,A]: stored open, 3 pairs
        seed("b_closed", VB, Pg, """{"x":[100,200,300,100],"y":[100,100,300,100]}""")
        // [A,B,A]: closing vertex dropped leaves 2 pairs -> insufficient_vertices
        seed("b_aba", VB, Pg, """{"x":[100,200,100],"y":[100,200,100]}""")
        seed("b_malformed", VB, BB, "not json")
        seed("b_mask", VB, "localization-mask", """{"rle":"x"}""")
        seed("b_nonloc", VB, "depth", """{"m":12}""")
        // V_C: no confidence at all
        seed("c_noconf", VC, BB, """{"x":0,"y":0,"width":100,"height":100}""")
        // V_D: 150 -> video is 0-100 -> 1.5 -> out of range
        seed("d_big", VD, BB, """{"x":0,"y":0,"width":100,"height":100,"confidence":150}""")
        // V_E: no video_references row, no image ref
        seed("e_nodim", VE, BB, """{"x":0,"y":0,"width":100,"height":100}""")
        // V_F: values on both sides of 1; the whole video is 0-100
        seed("f_mixed1", VF, BB, """{"x":0,"y":0,"width":100,"height":100,"confidence":0.9}""")
        seed("f_mixed2", VF, BB, """{"x":200,"y":0,"width":100,"height":100,"confidence":50}""")
        seed("b_case", VB, BB, """{"x":0,"y":0,"width":100,"height":100}""", "Self")

    /** Runs the routine; a dry run is rolled back. Returns outcome -> n. */
    private def runRoutine(dryRun: Boolean): Map[String, Long] =
        withConn { c =>
            c.setAutoCommit(false)
            try
                val out = mutable.LinkedHashMap.empty[String, Long]
                Using.resource(c.createStatement()) { s =>
                    val rs = s.executeQuery(routineSql)
                    while rs.next() do out(rs.getString(1)) = rs.getLong(2)
                }
                if dryRun then c.rollback() else c.commit()
                out.toMap
            catch
                case e: Throwable =>
                    c.rollback()
                    throw e
        }

    private def nLocalizations: Long = scalar("SELECT COUNT(*) FROM localizations")
    private def nRejects: Long       = scalar("SELECT COUNT(*) FROM localization_migration_rejects")

    private def rejectReasons: Map[String, Long] =
        withConn(c =>
            Using.resource(c.createStatement()) { s =>
                val rs  = s.executeQuery("SELECT reason, COUNT(*) FROM localization_migration_rejects GROUP BY reason")
                val out = mutable.Map.empty[String, Long]
                while rs.next() do out(rs.getString(1)) = rs.getLong(2)
                out.toMap
            }
        )

    /** (localization_type, confidence, part, vertices, width, height, x, y, training, truncated) of `tag`. */
    private case class Loc(
        tpe: String,
        confidence: Option[Double],
        part: Option[String],
        vertices: Option[String],
        width: Double,
        height: Double,
        x: Double,
        y: Double,
        training: Boolean,
        truncated: Boolean
    )

    private def loc(tag: String): Loc =
        withConn(c =>
            Using.resource(c.createStatement()) { s =>
                val rs = s.executeQuery(
                    s"""SELECT l.localization_type, l.confidence, l.part, l.vertices, l.width, l.height,
                       |       l.x, l.y, l.training, l.truncated
                       |FROM localizations l JOIN observations o ON o.uuid = l.observation_uuid
                       |WHERE o.concept = '$tag'""".stripMargin
                )
                assert(rs.next(), s"no localization for $tag")
                val conf = rs.getDouble(2)
                val confOpt = if rs.wasNull() then None else Some(conf)
                val l = Loc(
                    rs.getString(1),
                    confOpt,
                    Option(rs.getString(3)),
                    Option(rs.getString(4)),
                    rs.getDouble(5),
                    rs.getDouble(6),
                    rs.getDouble(7),
                    rs.getDouble(8),
                    rs.getBoolean(9),
                    rs.getBoolean(10)
                )
                assert(!rs.next(), s"more than one localization for $tag")
                l
            }
        )

    private def hasLoc(tag: String): Boolean =
        scalar(
            s"SELECT COUNT(*) FROM localizations l JOIN observations o ON o.uuid = l.observation_uuid WHERE o.concept = '$tag'"
        ) > 0

    private def assertClose(actual: Option[Double], expected: Double, clue: String): Unit =
        assert(actual.exists(a => math.abs(a - expected) < 1e-4), s"$clue: expected ~$expected but was $actual")

    private val Truncated = "  of which truncated (clamped to frame)"
    private val Orphan    = "info: orphaned image reference, used the video's dimensions (media assumption)"

    // ---- lifecycle ----------------------------------------------------------------------------

    private def wipe(): Unit =
        execute("DELETE FROM localization_migration_rejects")
        execute("DELETE FROM localizations")

    override def beforeAll(): Unit =
        super.beforeAll()
        // opening a DAO initializes the EntityManagerFactory, which creates/migrates the schema
        daoFactory.cleanup()
        createVideoReferencesTable.foreach(execute)

    override def afterAll(): Unit =
        try
            wipe()
            daoFactory.cleanup()
            execute(dropVideoReferencesTable)
        finally super.afterAll()

    override def beforeEach(context: BeforeEach): Unit =
        super.beforeEach(context)
        wipe()
        daoFactory.cleanup()
        execute(s"DELETE FROM $videoReferencesName")
        seedLegacy()

    override def afterEach(context: AfterEach): Unit =
        // localizations and rejects are not mapped by JPA: remove them before the JPA cleanup deletes observations
        wipe()
        super.afterEach(context)

    // ---- tests --------------------------------------------------------------------------------

    test("dry run, real run, second run and delta pass") {
        // dry run: the routine reports but leaves nothing behind
        val dry = runRoutine(dryRun = true)
        assertEquals(dry("migrated"), 16L)
        assertEquals(nLocalizations, 0L)
        assertEquals(nRejects, 0L)

        // real run
        val report = runRoutine(dryRun = false)
        assertEquals(report("migrated"), 16L)
        assertEquals(report(Truncated), 1L)
        assertEquals(report(Orphan), 1L)
        assertEquals(nLocalizations, 16L)
        assertEquals(nRejects, 11L)
        assertEquals(
            rejectReasons,
            Map(
                "array_length_mismatch"   -> 1L,
                "confidence_out_of_range" -> 1L,
                "duplicate_type"          -> 1L,
                "entirely_out_of_bounds"  -> 1L,
                "insufficient_vertices"   -> 2L,
                "invalid_geometry"        -> 1L,
                "malformed_json"          -> 2L,
                "unknown_link_name"       -> 1L,
                "unresolved_dimensions"   -> 1L
            )
        )
        for (reason, n) <- rejectReasons do assertEquals(report(s"rejected: $reason"), n, reason)
        // V_A, V_D, V_F are 0-100 (V_D's only row is then rejected as out of range)
        assertEquals(report.get("info: videos on the 0-100 confidence scale (divided by 100)"), Some(3L), report.toString)
        // a_bbox1, a_bbox2, a_low, f_mixed1, f_mixed2
        assertEquals(report.get("info: rows divided by 100 (video is 0-100)"), Some(5L), report.toString)
        // b_edge and the surviving b_dup
        assertEquals(report.get("info: rows used as is (video is 0-1)"), Some(2L), report.toString)

        // second run: everything migratable is already migrated
        val second = runRoutine(dryRun = false)
        assertEquals(second("migrated"), 0L)
        assertEquals(nLocalizations, 16L)

        // delta pass: a new legacy row in an already-migrated 0-100 video is still divided by 100
        seed("a_delta", VA, BB, """{"x":0,"y":0,"width":240,"height":300,"confidence":0.3}""")
        val third = runRoutine(dryRun = false)
        assertEquals(third("migrated"), 1L)
        assertClose(loc("a_delta").confidence, 0.003, "delta confidence")
        assertEquals(nLocalizations, 17L)
    }

    test("confidence is normalized per video") {
        runRoutine(dryRun = false)
        assertClose(loc("a_bbox1").confidence, 0.43, "43 in a 0-100 video")
        assertClose(loc("a_low").confidence, 0.005, "0.5 in a 0-100 video")
        assertClose(loc("b_edge").confidence, 0.87, "0.87 in a 0-1 video")
        assertClose(loc("f_mixed1").confidence, 0.009, "0.9 in a video that straddles 1")
        assertClose(loc("f_mixed2").confidence, 0.5, "50 in a video that straddles 1")
        assertEquals(loc("c_noconf").confidence, None)
        assert(!hasLoc("d_big"), "150 in a 0-100 video is 1.5 and must be rejected")
        assertEquals(rejectReasons.get("confidence_out_of_range"), Some(1L))
    }

    test("part: self is NULL, eye and Self are kept") {
        runRoutine(dryRun = false)
        assertEquals(loc("a_low").part, None)
        assertEquals(loc("a_bbox1").part, Some("eye"))
        assertEquals(loc("b_case").part, Some("Self"))
    }

    test("a polygon whose last two vertices repeat keeps 4 vertex pairs") {
        runRoutine(dryRun = false)
        val l = loc("a_poly")
        assertEquals(l.tpe, "polygon")
        val numbers = l.vertices.get.replaceAll("[\\[\\]\\s]", "").split(",").filter(_.nonEmpty)
        assertEquals(numbers.length, 8)
    }

    test("an explicitly closed polygon is stored open; [A,B,A] is rejected as insufficient_vertices") {
        runRoutine(dryRun = false)
        val l       = loc("b_closed")
        assertEquals(l.tpe, "polygon")
        val numbers = l.vertices.get.replaceAll("[\\[\\]\\s]", "").split(",").filter(_.nonEmpty).map(_.toDouble)
        assertEquals(numbers.length, 6, l.vertices)
        // 1000x500: (100,100) (200,100) (300,300) -> (0.1,0.2) (0.2,0.2) (0.3,0.6)
        assertEquals(numbers.toSeq, Seq(0.1, 0.2, 0.2, 0.2, 0.3, 0.6))
        assert(!hasLoc("b_aba"), "[A,B,A] must not migrate")
        val n = scalar(
            "SELECT COUNT(*) FROM localization_migration_rejects r JOIN observations o ON o.uuid = r.observation_uuid " +
                "WHERE o.concept = 'b_aba' AND r.reason = 'insufficient_vertices'"
        )
        assertEquals(n, 1L)
    }

    test("tags containing training set training; other rows are not training") {
        runRoutine(dryRun = false)
        assert(loc("a_bbox2").training, "a_bbox2 has tags [training, other]")
        for tag <- Seq("a_bbox1", "a_low", "a_point", "a_line", "a_poly", "b_edge", "b_closed", "c_noconf") do
            assert(!loc(tag).training, tag)
    }

    test("an edge-crossing box is clamped to the frame and flagged truncated") {
        runRoutine(dryRun = false)
        // 1000x500: x 950..1050 clamps to 0.95..1.0, y 0..100 is 0.0..0.2
        val l = loc("b_edge")
        assert(l.truncated, l.toString)
        assert(math.abs(l.x - 0.95) < 1e-9, l.toString)
        assert(math.abs(l.y - 0.0) < 1e-9, l.toString)
        assert(math.abs(l.width - 0.05) < 1e-9, l.toString)
        assert(math.abs(l.height - 0.2) < 1e-9, l.toString)
        assert(!loc("b_img_ok").truncated)
    }

    test("frame size comes from the image reference when it has one, else from the video") {
        runRoutine(dryRun = false)
        // video 1000x500: 200x100 box is 0.2 x 0.2
        val orphan = loc("b_orphan")
        assert(math.abs(orphan.width - 0.2) < 1e-6 && math.abs(orphan.height - 0.2) < 1e-6, orphan.toString)
        // image 2000x1000: 400x200 box is 0.2 x 0.2 (it would be 0.4 x 0.4 against the video)
        val ok = loc("b_img_ok")
        assert(math.abs(ok.width - 0.2) < 1e-6 && math.abs(ok.height - 0.2) < 1e-6, ok.toString)
        // sizeless image reference falls back to the video: 200x100 is 0.2 x 0.2
        val nosize = loc("b_img_nosize")
        assert(math.abs(nosize.width - 0.2) < 1e-6 && math.abs(nosize.height - 0.2) < 1e-6, nosize.toString)
    }

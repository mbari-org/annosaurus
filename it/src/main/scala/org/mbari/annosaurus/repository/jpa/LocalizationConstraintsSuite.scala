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

import org.mbari.annosaurus.controllers.TestUtils

import java.sql.SQLException
import java.util.UUID
import scala.jdk.CollectionConverters.*
import scala.util.Using

/**
 * Exercises the database constraints of the localizations table (V1.1.0 migration) with raw SQL, so the
 * behavior is identical on PostgreSQL and SQL Server.
 */
trait LocalizationConstraintsSuite extends BaseDAOSuite:

    given JPADAOFactory = daoFactory

    private def newObservationUuid(): UUID =
        val xs = TestUtils.create(1, 1, 0)
        xs.head.getObservations.asScala.head.getUuid

    private def execute(sql: String): Int =
        Using.resource(daoFactory.databaseConfig.newConnection()) { conn =>
            Using.resource(conn.createStatement())(stmt => stmt.executeUpdate(sql))
        }

    private def insert(
        observationUuid: UUID,
        localizationType: String,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        vertices: Option[String] = None,
        confidence: Option[Double] = None
    ): Int =
        val v = vertices.map(s => s"'$s'").getOrElse("NULL")
        val c = confidence.map(_.toString).getOrElse("NULL")
        execute(
            s"""INSERT INTO localizations
               |  (observation_uuid, localization_type, x, y, width, height, vertices, confidence)
               |VALUES ('$observationUuid', '$localizationType', $x, $y, $width, $height, $v, $c)""".stripMargin
        )

    private def assertRejected(thunk: => Any): Unit =
        intercept[SQLException](thunk)

    private def sqlOpt(v: Option[Any]): String = v match
        case None             => "NULL"
        case Some(s: String)  => s"'$s'"
        case Some(other)      => other.toString

    /** Inserts a mask row; each mask column can be left NULL to isolate the mask branch of ck_localizations__shape. */
    private def insertMask(
        observationUuid: UUID,
        encoding: Option[String] = Some("coco_rle"),
        data: Option[String] = Some("9220003"),
        widthPixels: Option[Int] = Some(6),
        heightPixels: Option[Int] = Some(4),
        vertices: Option[String] = None
    ): Int =
        execute(
            s"""INSERT INTO localizations
               |  (observation_uuid, localization_type, x, y, width, height, vertices,
               |   mask_encoding, mask_data, mask_width_pixels, mask_height_pixels)
               |VALUES ('$observationUuid', 'mask', 0.1, 0.1, 0.2, 0.2, ${sqlOpt(vertices)},
               |   ${sqlOpt(encoding)}, ${sqlOpt(data)}, ${sqlOpt(widthPixels)}, ${sqlOpt(heightPixels)})""".stripMargin
        )

    override def afterEach(context: AfterEach): Unit =
        // localizations are not mapped by JPA yet, so remove them before the JPA cleanup deletes observations
        execute("DELETE FROM localizations")
        super.afterEach(context)

    test("a valid bounding_box inserts") {
        val obs = newObservationUuid()
        assertEquals(insert(obs, "bounding_box", 0.1, 0.1, 0.2, 0.2), 1)
    }

    test("a second bounding_box for the same observation is rejected") {
        val obs = newObservationUuid()
        insert(obs, "bounding_box", 0.1, 0.1, 0.2, 0.2)
        assertRejected(insert(obs, "bounding_box", 0.3, 0.3, 0.2, 0.2))
    }

    test("a point with a width is rejected") {
        val obs = newObservationUuid()
        assertRejected(insert(obs, "point", 0.1, 0.1, 0.1, 0))
    }

    test("a bounding_box with vertices is rejected") {
        val obs = newObservationUuid()
        assertRejected(insert(obs, "bounding_box", 0.1, 0.1, 0.2, 0.2, vertices = Some("[0.1,0.1,0.3,0.3]")))
    }

    test("a polygon without vertices is rejected") {
        val obs = newObservationUuid()
        assertRejected(insert(obs, "polygon", 0.1, 0.1, 0.2, 0.2))
    }

    test("a confidence outside 0-1 is rejected") {
        val obs = newObservationUuid()
        assertRejected(insert(obs, "bounding_box", 0.1, 0.1, 0.2, 0.2, confidence = Some(43)))
    }

    test("an extent outside the frame is rejected") {
        val obs = newObservationUuid()
        assertRejected(insert(obs, "bounding_box", 0.5, 0.1, 1.0, 0.2))
    }

    test("an unknown localization_type is rejected") {
        val obs = newObservationUuid()
        assertRejected(insert(obs, "circle", 0.1, 0.1, 0.2, 0.2))
    }

    test("a point and a bounding_box on the same observation both insert") {
        val obs = newObservationUuid()
        assertEquals(insert(obs, "point", 0.1, 0.1, 0, 0), 1)
        assertEquals(insert(obs, "bounding_box", 0.1, 0.1, 0.2, 0.2), 1)
    }

    // ---- spec 10: the mask branch of ck_localizations__shape and the remaining extent checks ------------

    test("a complete mask inserts (positive control)") {
        assertEquals(insertMask(newObservationUuid()), 1)
    }

    test("a mask without mask_data is rejected") {
        assertRejected(insertMask(newObservationUuid(), data = None))
    }

    test("a mask without mask_encoding is rejected") {
        assertRejected(insertMask(newObservationUuid(), encoding = None))
    }

    test("a mask without mask_width_pixels is rejected") {
        assertRejected(insertMask(newObservationUuid(), widthPixels = None))
    }

    test("a mask without mask_height_pixels is rejected") {
        assertRejected(insertMask(newObservationUuid(), heightPixels = None))
    }

    test("a mask with vertices is rejected") {
        assertRejected(insertMask(newObservationUuid(), vertices = Some("[0.1,0.1,0.3,0.3]")))
    }

    test("a line with vertices inserts (positive control); a line without vertices is rejected") {
        assertEquals(insert(newObservationUuid(), "line", 0.1, 0.1, 0.2, 0.2, vertices = Some("[0.1,0.1,0.3,0.3]")), 1)
        assertRejected(insert(newObservationUuid(), "line", 0.1, 0.1, 0.2, 0.2))
    }

    test("x = 0 and y = 0 insert (positive control); x < 0 and y < 0 are rejected") {
        assertEquals(insert(newObservationUuid(), "bounding_box", 0.0, 0.0, 0.2, 0.2), 1)
        assertRejected(insert(newObservationUuid(), "bounding_box", -0.1, 0.1, 0.2, 0.2))
        assertRejected(insert(newObservationUuid(), "bounding_box", 0.1, -0.1, 0.2, 0.2))
    }

    test("a bounding_box with width/height at MIN_EXTENT inserts; width 0 or height 0 is rejected") {
        assertEquals(insert(newObservationUuid(), "bounding_box", 0.1, 0.1, 1e-9, 1e-9), 1)
        assertRejected(insert(newObservationUuid(), "bounding_box", 0.1, 0.1, 0.0, 0.2))
        assertRejected(insert(newObservationUuid(), "bounding_box", 0.1, 0.1, 0.2, 0.0))
    }

    test("difficult defaults to false") {
        val obs = newObservationUuid()
        insert(obs, "bounding_box", 0.1, 0.1, 0.2, 0.2)
        val difficult = Using.resource(daoFactory.databaseConfig.newConnection()) { conn =>
            Using.resource(conn.createStatement()) { stmt =>
                val rs = stmt.executeQuery(s"SELECT difficult FROM localizations WHERE observation_uuid = '$obs'")
                assert(rs.next())
                rs.getBoolean(1)
            }
        }
        assert(!difficult)
    }

    test("localized_annotations returns one row per localization") {
        val obs = newObservationUuid()
        insert(obs, "bounding_box", 0.1, 0.1, 0.2, 0.2)
        val (rows, columns) = Using.resource(daoFactory.databaseConfig.newConnection()) { conn =>
            Using.resource(conn.createStatement()) { stmt =>
                val rs      = stmt.executeQuery(s"SELECT * FROM localized_annotations WHERE observation_uuid = '$obs'")
                val md      = rs.getMetaData
                val columns = (1 to md.getColumnCount).map(i => md.getColumnName(i).toLowerCase).toSet
                var n       = 0
                while rs.next() do n = n + 1
                (n, columns)
            }
        }
        assertEquals(rows, 1)
        assert(columns.contains("ancillary_x"), s"ancillary_x not in $columns")
    }

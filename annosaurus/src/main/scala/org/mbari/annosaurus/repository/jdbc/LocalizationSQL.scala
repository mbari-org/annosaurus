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

package org.mbari.annosaurus.repository.jdbc

import org.mbari.annosaurus.domain.{Annotation, Localization}

/**
 * Raw SQL for reading localizations alongside annotations. Counterpart of AssociationSQL.
 */
object LocalizationSQL:

    /** Column order matters: resultListToLocalizations reads the columns by position. */
    val SELECT: String =
        """ SELECT
      |  loc.uuid AS localization_uuid,
      |  loc.observation_uuid,
      |  loc.localization_type,
      |  loc.x,
      |  loc.y,
      |  loc.width,
      |  loc.height,
      |  loc.vertices,
      |  loc.mask_encoding,
      |  loc.mask_data,
      |  loc.mask_width_pixels,
      |  loc.mask_height_pixels,
      |  loc.generator,
      |  loc.confidence,
      |  loc.observer,
      |  loc.observation_timestamp,
      |  loc.verifier,
      |  loc.verification_timestamp,
      |  loc.comment,
      |  loc.part,
      |  loc.training,
      |  loc.truncated,
      |  loc.difficult,
      |  loc.last_updated_timestamp
    """.stripMargin

    val FROM: String =
        """FROM
      |  imaged_moments im RIGHT JOIN
      |  observations obs ON obs.imaged_moment_uuid = im.uuid RIGHT JOIN
      |  localizations loc ON loc.observation_uuid = obs.uuid""".stripMargin

    val ORDER: String = " ORDER BY loc.uuid"

    val byVideoReferenceUuid: String = SELECT + FROM + " WHERE im.video_reference_uuid = ?" + ORDER

    val byVideoReferenceUuidBetweenDates: String = SELECT + FROM +
        " WHERE im.video_reference_uuid = ? AND im.recorded_timestamp BETWEEN ? AND ? " + ORDER

    val byConcurrentRequest: String = SELECT + FROM +
        " WHERE im.video_reference_uuid IN (?) AND im.recorded_timestamp BETWEEN ? AND ?" + ORDER

    val byMultiRequest: String = SELECT + FROM + " WHERE im.video_reference_uuid IN (?)" + ORDER

    val byObservationUuids: String = SELECT + FROM + " WHERE obs.uuid IN (?)" + ORDER

    val deleteByVideoReferenceUuid: String =
        """ DELETE FROM localizations WHERE EXISTS (
      |   SELECT
      |     *
      |   FROM
      |     imaged_moments im RIGHT JOIN
      |     observations obs ON obs.imaged_moment_uuid = im.uuid
      |   WHERE
      |     im.video_reference_uuid = ? AND
      |     obs.uuid = localizations.observation_uuid
      | )
      |""".stripMargin

    def resultListToLocalizations(rows: List[?]): Seq[Localization] =
        for row <- rows
        yield
            val xs = row.asInstanceOf[Array[Object]]
            Localization(
                localizationType = xs(2).asString.getOrElse(""),
                x = xs(3).asDouble,
                y = xs(4).asDouble,
                width = xs(5).asDouble,
                height = xs(6).asDouble,
                vertices = text(xs(7)).map(Localization.parseVertices),
                maskEncoding = xs(8).asString,
                maskData = text(xs(9)),
                maskWidthPixels = xs(10).asInt,
                maskHeightPixels = xs(11).asInt,
                generator = xs(12).asString,
                // The column is single precision (Float, Double or BigDecimal depending on the driver).
                // Widen through the decimal string so 0.9f reads back as 0.9, like Localization.from.
                confidence = xs(13).asFloat.map(_.toString.toDouble),
                observer = xs(14).asString,
                observationTimestamp = xs(15).asInstant,
                verifier = xs(16).asString,
                verificationTimestamp = xs(17).asInstant,
                comment = text(xs(18)),
                part = xs(19).asString,
                training = bool(xs(20)),
                truncated = bool(xs(21)),
                difficult = bool(xs(22)),
                uuid = xs(0).asUUID,
                observationUuid = xs(1).asUUID,
                lastUpdated = version(xs(23))
            )

    def join(
        annotations: Seq[Annotation],
        localizations: Seq[Localization]
    ): Seq[Annotation] =
        val byObservation = localizations.groupBy(_.observationUuid)
        for a <- annotations
        yield
            val matches = byObservation.getOrElse(a.observationUuid, Nil)
            if matches.isEmpty then a
            else a.copy(localizations = a.localizations.appendedAll(matches))

    /** PostgreSQL returns boolean as Boolean, SQL Server returns bit as Boolean or a Number. */
    private def bool(obj: Object): Boolean =
        obj match
            case b: java.lang.Boolean => b.booleanValue
            case n: Number            => n.intValue != 0
            case s: String            => s.equalsIgnoreCase("true") || s == "1"
            case _                    => false

    /** text/varchar(max) columns arrive as String or, for some drivers, Clob. */
    /**
     * last_updated_timestamp is a zone-less column (timestamp / datetime2), which a native query returns as a
     * LocalDateTime. Interpret it exactly as the JPA path does (a java.sql.Timestamp in the JVM's zone), so the token
     * read here equals the one the JPA path returns for the same row.
     */
    private def version(obj: Object): Option[java.time.Instant] =
        obj match
            case ldt: java.time.LocalDateTime => Some(java.sql.Timestamp.valueOf(ldt).toInstant)
            case _                            => obj.asInstant

    private def text(obj: Object): Option[String] =
        obj match
            case null              => None
            case c: java.sql.Clob  => Some(c.getSubString(1, c.length.toInt))
            case _                 => obj.asString

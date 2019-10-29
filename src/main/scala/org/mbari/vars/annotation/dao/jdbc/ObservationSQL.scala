package org.mbari.vars.annotation.dao.jdbc

/**
 * @author Brian Schlining
 * @since 2019-10-28T16:39:00
 */
object ObservationSQL {

  val deleteByVideoReferenceUuid: String =
    """ DELETE FROM observations WHERE EXISTS (
      |   SELECT
      |     *
      |   FROM
      |     imaged_moments im
      |   WHERE
      |     im.video_reference_uuid = ? AND
      |     im.uuid = observations.imaged_moment_uuid
      | )
      |""".stripMargin

}

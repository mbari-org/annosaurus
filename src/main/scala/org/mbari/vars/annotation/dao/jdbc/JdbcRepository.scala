package org.mbari.vars.annotation.dao.jdbc

import java.util.UUID

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import javax.sql.DataSource

class JdbcRepository(dataSource: DataSource) {

}

object AnnotationSQL {
  val SELECT: String =
    """ SELECT
      |  im.uuid AS imaged_moment_uuid,
      |  im.video_reference_uuid
      |  im.elapsed_time_millis,
      |  im.recorded_timestamp,
      |  im.timecode,
      |  obs.uuid AS observation_uuid,
      |  obs.concept,
      |  obs.activity,
      |  obs.observation_group,
      |  obs.observation_timestamp,
      |  obs.observer """.stripMargin

  val FROM: String =
    """ FROM
      |  imaged_moments im RIGHT JOIN
      |  observations obs ON obs.imaged_moment_uuid = im.uuid """.stripMargin

  val FROM_WITH_IMAGES: String =
    """ FROM
      |  imaged_moments im RIGHT JOIN
      |  observations obs ON obs.imaged_moment_uuid = im.uuid RIGHT JOIN
      |  image_references ir ON ir.imaged_moment_uuid = im.uuid """.stripMargin

  val all: String = SELECT + FROM

  val byVideoReferenceUuid: String = SELECT + FROM + " WHERE im.video_reference_uuid = ?"

  val byConcept: String = SELECT + FROM + " WHERE obs.concept = ?"

  val byConceptWithImages: String = SELECT + FROM_WITH_IMAGES +
    " WHERE ir.url IS NOT NULL AND obs.concept = ?"

  val betweenDates: String = SELECT + FROM +
    " WHERE im.recorded_timestamp BETWEEN ? AND ?"

  val byVideoReferenceUuidBetweenDates: String = SELECT + FROM +
    " WHERE im.video_reference_uuid = ? AND im.recorded_timestamp BETWEEN ? AND ? "

}

object AssociationSQL {
  val SELECT: String =
    """ SELECT
      |
    """.stripMargin
}

object JdbcRepository {




  val dataSource: DataSource = {
    val config = ConfigFactory.load()
    val environment = config.getString("database.environment")
    val nodeName = if (environment.equalsIgnoreCase("production")) "org.mbari.vars.annotation.database.production"
    else "org.mbari.vars.annotation.database.development"
    val hikariConfig = new HikariConfig
    val url = config.getString(nodeName + ".url")
    val user = config.getString(nodeName + ".user")
    val password = config.getString(nodeName + ".password")
    hikariConfig.setJdbcUrl(url)
    hikariConfig.setUsername(user)
    hikariConfig.setPassword(password)
    hikariConfig.setMaximumPoolSize(Runtime.getRuntime.availableProcessors * 2)

    new HikariDataSource(hikariConfig)
  }
}

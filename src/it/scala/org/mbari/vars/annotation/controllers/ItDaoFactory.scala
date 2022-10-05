package org.mbari.vars.annotation.controllers

import org.mbari.vars.annotation.dao.jpa.JPADAOFactory
import scala.concurrent.Await
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

trait ItDaoFactory extends JPADAOFactory {
  val TestProperties: Map[String, String] = Map(
    "eclipselink.logging.level.sql"                             -> "FINE",
    "eclipselink.logging.parameters"                            -> "true",
    "eclipselink.logging.level"                                 -> "INFO",
    "javax.persistence.schema-generation.scripts.action"        -> "none",
    "javax.persistence.schema-generation.database.action"       -> "none",
    "javax.persistence.schema-generation.scripts.create-target" -> "target/test-database-create.ddl"
  )

  def cleanup(): Unit = {

    import scala.concurrent.ExecutionContext.Implicits.global
    val dao = newImagedMomentDAO()

    val f = dao.runTransaction(_ => {
      val all = dao.findAll()
      all.foreach(dao.delete)
    })
    f.onComplete(_ => dao.close())
    Await.result(f, Duration(400, TimeUnit.SECONDS))

  }

  def testProps(): Map[String, String]
}

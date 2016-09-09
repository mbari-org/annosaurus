package org.mbari.vars.annotation.dao.jpa

import java.util.concurrent.TimeUnit
import javax.persistence.EntityManagerFactory

import com.typesafe.config.ConfigFactory
import org.eclipse.persistence.config.TargetDatabase

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import org.mbari.vars.annotation.dao.jpa.Implicits.RichEntityManager

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-05-06T11:04:00
 */
object DerbyTestDAOFactory extends JPADAOFactory {

  private[this] val config = ConfigFactory.load()
  private[this] val testProps = Map(
    "eclipselink.connection-pool.default.initial" -> "2",
    "eclipselink.connection-pool.default.max" -> "16",
    "eclipselink.connection-pool.default.min" -> "2",
    "eclipselink.logging.level" -> "FINE",
    "eclipselink.logging.session" -> "false",
    "eclipselink.logging.thread" -> "false",
    "eclipselink.logging.timestamp" -> "false",
    "eclipselink.target-database" -> TargetDatabase.Derby,
    "javax.persistence.database-product-name" -> TargetDatabase.Derby,
    "javax.persistence.schema-generation.database.action" -> "create",
    "javax.persistence.schema-generation.scripts.action" -> "drop-and-create",
    "javax.persistence.schema-generation.scripts.create-target" -> "target/test-database-create.ddl",
    "javax.persistence.schema-generation.scripts.drop-target" -> "target/test-database-drop.ddl"
  //"eclipselink.ddl-generation" -> "create-tables",
  //"eclipselink.ddl-generation.output-mode" -> "database"
  )

  lazy val entityManagerFactory: EntityManagerFactory = {
    val driver = config.getString("org.mbari.vars.annotation.database.derby.driver")
    val url = config.getString("org.mbari.vars.annotation.database.derby.url")
    val user = config.getString("org.mbari.vars.annotation.database.derby.user")
    val password = config.getString("org.mbari.vars.annotation.database.derby.password")
    EntityManagerFactories(url, user, password, driver, testProps)
  }

  def cleanup(): Unit = {

    import scala.concurrent.ExecutionContext.Implicits.global
    val dao = newImagedMomentDAO()

    val f = dao.runTransaction(d => {
      val all = dao.findAll()
      all.foreach(dao.delete)
    })
    f.onComplete(t => dao.close())
    Await.result(f, Duration(4, TimeUnit.SECONDS))

  }

}

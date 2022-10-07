package org.mbari.vars.annotation.controllers

import org.scalatest.flatspec.AnyFlatSpec
import com.dimafeng.testcontainers.ForAllTestContainer
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.eclipse.persistence.config.TargetDatabase
import org.mbari.vars.annotation.dao.jpa.EntityManagerFactories

import java.sql.DriverManager
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.persistence.EntityManagerFactory
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration => SDuration}
import org.slf4j.bridge.SLF4JBridgeHandler
import org.mbari.vars.annotation.dao.jpa.DatabaseProductName

class PGAnnotationControllerSpec
    extends AnyFlatSpec
    with ForAllTestContainer
    with Matchers
    with BeforeAndAfterAll {

  SLF4JBridgeHandler.removeHandlersForRootLogger()
  SLF4JBridgeHandler.install()
  DatabaseProductName.usePostgreSQL()

  override val container: PostgreSQLContainer = PostgreSQLContainer("postgres:14")
  container.start()

  val jdbcUrl = s"${container.jdbcUrl}?sslmode=disable&stringType=unspecified&autoReconnect=true"

  val daoFactory: BasicDAOFactory = new ItDaoFactory {
    override def testProps(): Map[String, String] =
      TestProperties ++
        Map(
          "eclipselink.target-database"             -> TargetDatabase.PostgreSQL,
          "javax.persistence.database-product-name" -> TargetDatabase.PostgreSQL
        )

    override lazy val entityManagerFactory: EntityManagerFactory = {
      EntityManagerFactories(
        jdbcUrl,
        container.username,
        container.password,
        container.driverClassName,
        testProps()
      )
    }
  }.asInstanceOf[BasicDAOFactory]

  private[this] val controller = new AnnotationController(daoFactory)
  private[this] val timeout    = SDuration(200, TimeUnit.SECONDS)
  private[this] val videoReferenceUuid = UUID.randomUUID()

  def exec[R](fn: () => Future[R]): R = Await.result(fn.apply(), timeout)

  it should "be postgresql" in {
    DatabaseProductName.isPostgreSQL() should be(true)
  }

  it should "connect to postgresql" in {
    Class.forName(container.driverClassName)
    val connection = DriverManager.getConnection(jdbcUrl, container.username, container.password)
    connection should not be null
    connection.close()
  }

  it should "configure database" in {
    val src        = getClass().getResource("/postgres.ddl")
    val sql        = Source.fromURL(src).mkString
    val connection = DriverManager.getConnection(jdbcUrl, container.username, container.password)

    // setup tables
    TestUtil.runDdl(sql, connection)

    // make sure the tables were created. We just check a count in one of them
    val statement = connection.createStatement()
    val rs = statement.executeQuery("SELECT COUNT(*) FROM observations")
    rs should not be (null)
    while (rs.next()) {
      rs.getInt(1) should be (0)
    }
    statement.close()

    connection.close()
  }

  it should "create" in {
    val recordedDate = Instant.now()
    val a = exec(() =>
      controller
        .create(videoReferenceUuid, "Nanomia bijuga", "brian", recordedDate = Some(recordedDate))
    )
    a.concept should be("Nanomia bijuga")
    a.observer should be("brian")
    a.recordedTimestamp should be(recordedDate)
    a.videoReferenceUuid should be (videoReferenceUuid)
  }

  it should "update" in {
    val xs = exec(() => controller.findByVideoReferenceUUID(videoReferenceUuid))
    xs.size should be (1)
    val a = xs.head
    val opt = exec(() => controller.update(a.observationUuid, concept = Some("Pandalus")))
    opt should not be empty
    val b = opt.get
    b.concept should be ("Pandalus")
    val ys = exec(() => controller.findByVideoReferenceUUID(videoReferenceUuid))
    ys.size should be(1)
    val c = ys.head
    c.concept should be("Pandalus")
  }

  it should "delete" in {
    val xs = exec(() => controller.findByVideoReferenceUUID(videoReferenceUuid))
    xs.size should be(1)
    val a = xs.head
    exec(() => controller.delete(a.observationUuid))
    val ys = exec(() => controller.findByVideoReferenceUUID(videoReferenceUuid))
    ys should be (empty)
  }
}

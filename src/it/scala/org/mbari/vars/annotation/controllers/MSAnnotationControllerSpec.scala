package org.mbari.vars.annotation.controllers

import org.scalatest.flatspec.AnyFlatSpec
import com.dimafeng.testcontainers.{ForAllTestContainer, MSSQLServerContainer, PostgreSQLContainer}
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import org.eclipse.persistence.config.TargetDatabase
import org.mbari.vars.annotation.dao.jpa.{DatabaseProductName, EntityManagerFactories}

import java.sql.DriverManager
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.persistence.EntityManagerFactory
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration => SDuration}
import scala.io.Source

class MSAnnotationControllerSpec
    extends AnyFlatSpec
    with ForAllTestContainer
    with Matchers
    with BeforeAndAfterAll {

  DatabaseProductName.useSQLServer()

  import org.testcontainers.utility.DockerImageName

  val myImage: DockerImageName = DockerImageName.parse("mcr.microsoft.com/azure-sql-edge").asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server")
  override val container: MSSQLServerContainer = MSSQLServerContainer(myImage)
  container.start()

  val jdbcUrl = s"${container.jdbcUrl}"

  val daoFactory: BasicDAOFactory = new ItDaoFactory {
    override def testProps(): Map[String, String] =
      TestProperties ++
        Map(
          "eclipselink.target-database"             -> TargetDatabase.SQLServer,
          "javax.persistence.database-product-name" -> TargetDatabase.SQLServer
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

  it should "connect to sqlserver" in {
    Class.forName(container.driverClassName)
    val connection = DriverManager.getConnection(jdbcUrl, container.username, container.password)
    connection should not be null
    connection.close()
  }

  it should "configure database" in {
    val src = getClass().getResource("/sqlserver.ddl")
    val sql = Source.fromURL(src).mkString
    val connection = DriverManager.getConnection(jdbcUrl, container.username, container.password)

    // setup tables
    TestUtil.runDdl(sql, connection)

    // make sure the tables were created. We just check a count in one of them
    val statement = connection.createStatement()
    val rs = statement.executeQuery("SELECT COUNT(*) FROM observations")
    rs should not be (null)
    while (rs.next()) {
      rs.getInt(1) should be(0)
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
    a.videoReferenceUuid should be(videoReferenceUuid)
  }

  it should "update" in {
    val xs = exec(() => controller.findByVideoReferenceUUID(videoReferenceUuid))
    xs.size should be(1)
    val a = xs.head
    val opt = exec(() => controller.update(a.observationUuid, concept = Some("Pandalus")))
    opt should not be empty
    val b = opt.get
    b.concept should be("Pandalus")
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
    ys should be(empty)
  }
}

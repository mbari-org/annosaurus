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

  def exec[R](fn: () => Future[R]): R = Await.result(fn.apply(), timeout)

  it should "connect to sqlserver" in {
    Class.forName(container.driverClassName)
    val connection = DriverManager.getConnection(jdbcUrl, container.username, container.password)
    connection should not be null
  }

  it should "configure database" in {
    val src        = getClass().getResource("/sqlserver.ddl")
    val sql        = Source.fromURL(src).mkString
    val connection = DriverManager.getConnection(jdbcUrl, container.username, container.password)
    val statement  = connection.createStatement()
    statement.execute(sql)
    connection.close()
  }

  it should "create" in {
    val recordedDate = Instant.now()
    val a = exec(() =>
      controller
        .create(UUID.randomUUID(), "Nanomia bijuga", "brian", recordedDate = Some(recordedDate))
    )
    a.concept should be("Nanomia bijuga")
    a.observer should be("brian")
    a.recordedTimestamp should be(recordedDate)
  }

  it should "update" in {}

  it should "delete" in {}
}

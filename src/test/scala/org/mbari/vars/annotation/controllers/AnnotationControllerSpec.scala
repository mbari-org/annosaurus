package org.mbari.vars.annotation.controllers

import java.time.{ Duration, Instant }
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.dao.jpa.TestDAOFactory
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers }

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.{ Duration => SDuration }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author Brian Schlining
 * @since 2017-01-23T15:12:00
 */
class AnnotationControllerSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory = TestDAOFactory.Instance
  private[this] val controller = new AnnotationController(daoFactory.asInstanceOf[BasicDAOFactory])
  private[this] val timeout = SDuration(200, TimeUnit.SECONDS)
  private[this] val recordedDate = Instant.now()

  def exec[R](fn: () => Future[R]): R = Await.result(fn.apply(), timeout)

  "AnnotationController" should "create by recorded timestamp" in {
    val a = exec(() => controller.create(
      UUID.randomUUID(),
      "Nanomia bijuga",
      "brian",
      recordedDate = Some(recordedDate)
    ))
    a.concept should be("Nanomia bijuga")
    a.observer should be("brian")
    a.recordedTimestamp should be(recordedDate)
  }

  it should "create using existing recorded timestamp" in {
    val a = exec(() => controller.create(
      UUID.randomUUID(),
      "Grimpoteuthis",
      "brian",
      recordedDate = Some(recordedDate)
    ))
    a.concept should be("Grimpoteuthis")
    a.observer should be("brian")
    a.recordedTimestamp should be(recordedDate)
  }

  it should "create and update" in {
    val a = exec(() => controller.create(
      UUID.randomUUID(),
      "Grimpoteuthis",
      "brian",
      recordedDate = Some(recordedDate)
    ))
    a.concept should be("Grimpoteuthis")
    a.observer should be("brian")
    a.recordedTimestamp should be(recordedDate)

    val b = exec(() => controller.update(a.observationUuid, Some(a.videoReferenceUuid),
      concept = Some("Nanomia bijuga")))
    b should not be (empty)
    b.get.concept should be("Nanomia bijuga")
  }

  it should "create and update with different elapsed times" in {
    val et0 = Duration.ofSeconds(30)
    val et1 = Duration.ofSeconds(60)
    val a = exec(() => controller.create(
      UUID.randomUUID(),
      "Grimpoteuthis",
      "brian",
      elapsedTime = Some(et0)
    ))
    a.concept should be("Grimpoteuthis")
    a.observer should be("brian")
    a.elapsedTime should be(et0)

    val b = exec(() => controller.update(
      a.observationUuid,
      Some(a.videoReferenceUuid),
      concept = Some("Nanomia bijuga"),
      elapsedTime = Some(et1)
    ))
    b should not be (empty)
    b.get.concept should be("Nanomia bijuga")
    b.get.elapsedTime should be(et1)

    val c = exec(() => controller.update(
      a.observationUuid,
      elapsedTime = Some(et0)
    ))
    c should not be (empty)
    c.get.elapsedTime should be(et0)
  }

  protected override def afterAll(): Unit = {
    daoFactory.cleanup()
  }

}

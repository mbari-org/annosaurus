package org.mbari.vars.annotation.controllers

import java.time.Instant
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
  private[this] val timeout = SDuration(2, TimeUnit.SECONDS)
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

  protected override def afterAll(): Unit = {
    daoFactory.cleanup()
  }

}

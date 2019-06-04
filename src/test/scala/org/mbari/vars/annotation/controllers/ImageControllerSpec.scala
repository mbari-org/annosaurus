package org.mbari.vars.annotation.controllers

import java.net.URL
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.dao.jpa.TestDAOFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration => SDuration}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author Brian Schlining
 * @since 2019-06-04T09:31:00
 */
class ImageControllerSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory = TestDAOFactory.Instance
  private[this] val controller = new ImageController(daoFactory.asInstanceOf[BasicDAOFactory])
  private[this] val timeout = SDuration(200, TimeUnit.SECONDS)
  private[this] val recordedDate = Instant.now
  private[this] val urlPng = new URL("http://www.mbari.org/foo.png")
  private[this] val urlJpg = new URL("http://www.mbari.org/foo.jpg")
  private[this] val videoReferenceUuid = UUID.randomUUID()

  def exec[R](fn: () => Future[R]): R = Await.result(fn.apply(), timeout)

  "ImageController" should "create" in {
    val a = exec(() => controller.create(videoReferenceUuid,
      urlPng,
      recordedDate = Some(recordedDate),
      format = Some("image/png"),
      width = Some(1920),
      height = Some(1080),
      description = None))

    a.recordedTimestamp should be (recordedDate)
    a.url should be (urlPng)
  }

  it should "find by name" in {
    val a = exec(() => controller.findByImageName("foo"))
    a.size should be (1)
    a.head.url should be (urlPng)

    exec(() => controller.create(videoReferenceUuid,
      urlJpg,
      recordedDate = Some(recordedDate),
      format = Some("image/jpg"),
      width = Some(1920),
      height = Some(1080),
      description = None))

    val b = exec(() => controller.findByImageName("foo"))
    b.size should be (2)
    println(b)
    b.head.url should not be b.last.url

    val c = exec(() => controller.findByImageName("foo.png"))
    c.size should be (1)
    c.head.url should be (urlPng)
    println(c)

    val d = exec(() => controller.findByImageName("foo.jpg"))
    d.size should be (1)
    d.head.url should be (urlJpg)


  }

  protected override def afterAll(): Unit = {
    daoFactory.cleanup()
  }

}

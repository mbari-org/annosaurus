package org.mbari.vars.annotation.controllers

import java.time.{Duration, Instant}
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.dao.jpa.TestDAOFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration => SDuration}
import scala.concurrent.ExecutionContext.Implicits.global

class ImagedMomentControllerSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory = TestDAOFactory.Instance
  private[this] val controller = new ImagedMomentController(daoFactory.asInstanceOf[BasicDAOFactory])
  private[this] val recordedDate = Instant.now()
  private[this] val log = LoggerFactory.getLogger(getClass)
  private[this] val timeout = SDuration(200, TimeUnit.SECONDS)
  private[this] val videoReferenceUuid = UUID.randomUUID()

  def exec[R](fn: () => Future[R]): R = Await.result(fn.apply(), timeout)

  "ImagedMomentController" should "create by recorded timestamp" in {
    val a = exec(() => controller.create(videoReferenceUuid, recordedDate = Some(recordedDate)))
    a.recordedDate should be (recordedDate)
    a.timecode should be (null)
    a.elapsedTime should be (null)
    a.videoReferenceUUID should be (videoReferenceUuid)
  }

  it should "find by videoReferenceUuid and recordedDate" in {
    val a = ImagedMomentController.findImagedMoment(controller.newDAO(),
      videoReferenceUuid,
      recordedDate = Some(recordedDate))
    a should not be (null)
  }

  it should "create one imagedmoment if multiple creates use the same recordedDate" in {
    val a = exec(() => controller.create(videoReferenceUuid, recordedDate = Some(recordedDate)))
    val b = exec(() => controller.create(videoReferenceUuid, recordedDate = Some(recordedDate)))
    a.uuid should be (b.uuid)
  }

  it should "create one imagedmoment if multiple creates use the same elasped_time" in {
    val elapsedTime = Duration.ofMillis(123456)
    val a = exec(() => controller.create(videoReferenceUuid, elapsedTime = Some(elapsedTime)))
    val b = exec(() => controller.create(videoReferenceUuid, elapsedTime = Some(elapsedTime)))
    a.uuid should be (b.uuid)
  }

  it should "stream/find video_reference_uuids modified between dates" in {
    val end = Instant.now()
    val start = end.minus(Duration.ofMinutes(2))
    val (closeable, stream) = controller.streamVideoReferenceUuidsBetweenUpdatedDates(start, end)
    val uuids = stream.iterator()
      .asScala
      .toSeq

    uuids.size should not be 0
  }

  protected override def afterAll(): Unit = {
    daoFactory.cleanup()
  }

}

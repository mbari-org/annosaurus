package org.mbari.vars.annotation.dao.jpa

import java.time.{ Duration, Instant }
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vcr4j.time.Timecode
import org.scalatest.{ FlatSpec, Matchers }

import scala.concurrent.{ Await, Awaitable, Future }
import scala.concurrent.duration.{ Duration => SDuration }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-27T15:12:00
 */
class ImagedMomentDAOSpec extends FlatSpec with Matchers {

  private[this] val timeout = SDuration(2, TimeUnit.SECONDS)
  private[this] val dao = H2TestDAOFactory.newImagedMomentDAO()
  private[this] val videoReferenceUUID = UUID.randomUUID()
  private[this] val now = Instant.now()
  private[this] val imagedMoment0 = ImagedMomentImpl(Some(videoReferenceUUID), Some(now), elapsedTime = Some(Duration.ofMinutes(1)))
  private[this] val imagedMoment1 = ImagedMomentImpl(Some(videoReferenceUUID), Some(now.plusSeconds(60)), elapsedTime = Some(Duration.ofMinutes(5)))

  def run[R](fn: Awaitable[R]): R = Await.result(fn, timeout)

  "ImagedMomentDAOImpl" should "create" in {
    run(dao.runTransaction(d => d.create(imagedMoment0)))
    imagedMoment0.uuid should not be null

    // --- Add a second
    run(dao.runTransaction(d => d.create(imagedMoment1)))
  }

  it should "update" in {
    val timecode = new Timecode(2345, 29.97)
    run(dao.runTransaction(d => {
      val im = d.findByVideoReferenceUUID(videoReferenceUUID).headOption
      im shouldBe defined
      im.get.timecode = timecode
    }))

    val imagedMoment = run(dao.runTransaction(d => d.findByVideoReferenceUUID(videoReferenceUUID)))
      .filter(_.uuid == imagedMoment0.uuid)
      .head
    imagedMoment.timecode.toString should be(timecode.toString)
  }

  it should "findByUUID" in {
    val im = run(dao.runTransaction(d => d.findByUUID(imagedMoment0.uuid)))
    im shouldBe defined
  }

  it should "findByVideoReferenceUUIDAndElapsedTime" in {
    val im = run(dao.runTransaction(d => d.findByVideoReferenceUUIDAndElapsedTime(imagedMoment0.uuid, imagedMoment0.elapsedTime)))
    im shouldBe defined
  }

  it should "findByVideoReferenceUUIDAndRecordedDate" in {
    val im = run(dao.runTransaction(d => d.findByVideoReferenceUUIDAndRecordedDate(imagedMoment0.uuid, imagedMoment0.recordedDate)))
    im shouldBe defined
  }

  it should "findByVideoReferenceUUIDAndIndex" in {
    val im0 = run(dao.runTransaction(d => d.findByVideoReferenceUUIDAndIndex(videoReferenceUUID, elapsedTime = Option(imagedMoment0.elapsedTime))))
    im0 shouldBe defined
    val im1 = run(dao.runTransaction(d => d.findByVideoReferenceUUIDAndIndex(videoReferenceUUID, recordedDate = Option(imagedMoment0.recordedDate))))
    im1 shouldBe defined
  }

  it should "findAll" in {
    run(dao.runTransaction(d => d.create(imagedMoment1)))
    val all = run(dao.runTransaction(d => d.findAll()))
    all.size should be >= 2
  }

  it should "deleteByUUID" in {
    run(dao.runTransaction(d => d.delete(imagedMoment0)))
    val im = run(dao.runTransaction(d => d.findByUUID(imagedMoment0.uuid)))
    im shouldBe empty
  }

  it should "delete" in {
    val im = run(dao.runTransaction(d => d.findAll())).filter(_.uuid == imagedMoment1.uuid)
    run(dao.runTransaction(d => im.foreach(d.delete)))
    val imCheck = run(dao.runTransaction(d => d.findAll())).filter(_.uuid == imagedMoment1.uuid)
    imCheck shouldBe empty
  }

}

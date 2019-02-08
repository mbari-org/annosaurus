package org.mbari.vars.annotation.dao.jpa

import java.time.{Duration, Instant}
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.dao.{ImagedMomentDAO, IndexDAO}
import org.mbari.vcr4j.time.Timecode
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration => SDuration}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author Brian Schlining
  * @since 2019-02-08T09:18:00
  */
class IndexDAOSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  private type IMDAO = ImagedMomentDAO[ImagedMomentImpl]
  private type IDAO = IndexDAO[IndexImpl]
  private[this] val daoFactory = TestDAOFactory.Instance
  private[this] val timeout = SDuration(2, TimeUnit.SECONDS)
  private[this] val imDao = daoFactory.newImagedMomentDAO()
  private[this] val dao = daoFactory.newIndexDAO(imDao)
  private[this] val videoReferenceUUID = UUID.randomUUID()
  private[this] val now = Instant.now()
  private[this] val imagedMoment0 = ImagedMomentImpl(Some(videoReferenceUUID), Some(now), elapsedTime = Some(Duration.ofMinutes(1)))
  private[this] val imagedMoment1 = ImagedMomentImpl(Some(videoReferenceUUID), Some(now.plusSeconds(60)), elapsedTime = Some(Duration.ofMinutes(5)))



  def runIm[R](fn: IMDAO => R): R = Await.result(imDao.runTransaction(fn), timeout)

  def runId[R](fn: IDAO => R): R = Await.result(dao.runTransaction(fn), timeout)

  "ImagedMomentDAOImpl" should "create for spec setup" in {
    runIm(_.create(imagedMoment0))
    imagedMoment0.uuid should not be null

    // --- Add a second
    runIm(_.create(imagedMoment1))
  }

  it should "findByVideoReferenceUuid" in {
    val im = dao.findByVideoReferenceUuid(videoReferenceUUID)
    im should not be empty
    im.size should be (2)
    //im.foreach(println)
  }

  it should "update" in {
    val timecode = new Timecode(2345, 29.97)
    runId(d => {
      val id = d.findByVideoReferenceUuid(videoReferenceUUID)
          .find(_.uuid == imagedMoment0.uuid)
      id shouldBe defined
      id.get.timecode = timecode
    })

    val imagedMoment = runId(_.findByVideoReferenceUuid(videoReferenceUUID))
      .find(_.uuid == imagedMoment0.uuid)
    imagedMoment should not be empty
    imagedMoment.get.timecode should not be null
    imagedMoment.get.timecode.toString should be(timecode.toString)
  }

  protected override def afterAll(): Unit = {
    daoFactory.cleanup()
  }

}

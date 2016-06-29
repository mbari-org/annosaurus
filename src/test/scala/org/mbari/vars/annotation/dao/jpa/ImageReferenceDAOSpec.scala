package org.mbari.vars.annotation.dao.jpa

import java.net.URL
import java.time.{ Duration, Instant }
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.dao.ImageReferenceDAO
import org.scalatest.{ FlatSpec, Matchers }

import scala.concurrent.{ Await, Awaitable }
import scala.concurrent.duration.{ Duration => SDuration }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-28T17:04:00
 */
class ImageReferenceDAOSpec extends FlatSpec with Matchers {

  private[this] val timeout = SDuration(2, TimeUnit.SECONDS)
  private[this] val imDao = H2TestDAOFactory.newImagedMomentDAO()
  private[this] val dao = H2TestDAOFactory.newImageReferenceDAO(imDao)
  private[this] val videoReferenceUUID = UUID.randomUUID()
  private[this] val now = Instant.now()
  private[this] val imagedMoment0 = ImagedMomentImpl(Some(videoReferenceUUID), Some(now), elapsedTime = Some(Duration.ofMinutes(1)))
  private[this] val imageReference0 = ImageReferenceImpl(new URL("http://www.mbari.org/wp-content/uploads/2015/08/schlining_brian-180.jpg"))
  private[this] val imageReference1 = ImageReferenceImpl(new URL("https://afleetinglance.files.wordpress.com/2012/07/zazen2.jpg"))
  private[this] val newDescription = "A handsome fellow"

  private type IRDAO = ImageReferenceDAO[ImageReferenceImpl]
  def run[R](fn: IRDAO => R): R = Await.result(dao.runTransaction(fn), timeout)

  "ImageReferenceDAOImpl" should "create" in {
    imagedMoment0.addImageReference(imageReference0)
    run(_.create(imageReference0))
    imageReference0.uuid should not be null

    imagedMoment0.addImageReference(imageReference1)
    run(_.create(imageReference0))
  }

  it should "update" in {
    run(d => {
      val ir = d.findByUUID(imageReference0.uuid)
      ir shouldBe defined
      ir.get.description = newDescription
    })

    val ir = run(_.findByUUID(imageReference0.uuid)).head
    ir.description should be(newDescription)

  }

  it should "findAll" in {
    val irs = run(_.findAll()).filter(_.imagedMoment.uuid == imagedMoment0.uuid)
    irs.size should be(2)
  }

  it should "deleteByUUID" in {
    run(_.deleteByUUID(imageReference1.uuid))
    val ir1 = run(_.findByUUID(imageReference1.uuid))
    ir1 shouldBe empty
  }

  it should "delete" in {
    run(_.delete(imageReference0))
    val ir0 = run(_.findByUUID(imageReference0.uuid))
    ir0 shouldBe empty
  }

}

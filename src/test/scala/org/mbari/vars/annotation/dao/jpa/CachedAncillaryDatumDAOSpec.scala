package org.mbari.vars.annotation.dao.jpa

import java.time.{ Duration, Instant }
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.dao.{ CachedAncillaryDatumDAO, ImageReferenceDAO }
import org.scalatest.{ FlatSpec, Matchers }

import scala.concurrent.{ Await, Awaitable }
import scala.concurrent.duration.{ Duration => SDuration }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-28T15:39:00
 */
class CachedAncillaryDatumDAOSpec extends FlatSpec with Matchers {

  private[this] val timeout = SDuration(2, TimeUnit.SECONDS)
  private[this] val imDao = H2TestDAOFactory.newImagedMomentDAO()
  private[this] val dao = H2TestDAOFactory.newCachedAncillaryDatumDAO(imDao)
  private[this] val videoReferenceUUID = UUID.randomUUID()
  private[this] val now = Instant.now()
  private[this] val imagedMoment0 = ImagedMomentImpl(Some(videoReferenceUUID), Some(now), elapsedTime = Some(Duration.ofMinutes(1)))
  private[this] val ancillaryDatum0 = CachedAncillaryDatumImpl(36.234, 122.0011, 666)
  private[this] val newTemp = 3.2F

  private type IRDAO = CachedAncillaryDatumDAO[CachedAncillaryDatumImpl]
  def run[R](fn: IRDAO => R): R = Await.result(dao.runTransaction(fn), timeout)

  "CachedAncillaryDatumDAOImpl" should "create" in {
    imagedMoment0.ancillaryDatum = ancillaryDatum0
    run(_.create(ancillaryDatum0))
    ancillaryDatum0.uuid should not be null
  }

  it should "update" in {
    run(d => {
      val ad = d.findByUUID(ancillaryDatum0.uuid)
      ad shouldBe defined
      ad.get.temperatureCelsius = newTemp
    })

    val datum = run(d => d.findByUUID(ancillaryDatum0.uuid)).head
    datum.temperatureCelsius should be(newTemp +- 0.000001F)
  }

  it should "findAll" in {
    val datum = run(_.findAll()).filter(_.imagedMoment.uuid == imagedMoment0.uuid)
    datum.size should be(1)
  }

  it should "delete" in {
    run(d => {
      val datum = d.findByUUID(ancillaryDatum0.uuid)
      d.delete(datum.get)
    })

    val datCheck = run(_.findByUUID(ancillaryDatum0.uuid))
    datCheck shouldBe empty
  }

}

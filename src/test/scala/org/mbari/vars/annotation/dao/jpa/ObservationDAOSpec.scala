package org.mbari.vars.annotation.dao.jpa

import java.time.{ Duration, Instant }
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.dao.{ ImagedMomentDAO, ObservationDAO }
import org.scalatest.{ FlatSpec, Matchers, BeforeAndAfterAll }

import scala.concurrent.{ Await, Awaitable }
import scala.concurrent.duration.{ Duration => SDuration }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-28T08:44:00
 */
class ObservationDAOSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val timeout = SDuration(2, TimeUnit.SECONDS)
  private[this] val imDao = H2TestDAOFactory.newImagedMomentDAO()
  private[this] val dao = H2TestDAOFactory.newObservationDAO(imDao)
  private[this] val videoReferenceUUID = UUID.randomUUID()
  private[this] val now = Instant.now()
  private[this] val imagedMoment0 = ImagedMomentImpl(Some(videoReferenceUUID), Some(now), elapsedTime = Some(Duration.ofMinutes(1)))
  private[this] val concept = "Grimpoteuthis"
  val newConcept = "Aegina"
  private[this] val observation0 = ObservationImpl(concept, observationDate = Some(now), observer = Some("brian"))
  private[this] val observation1 = ObservationImpl(concept, observationDate = Some(now), observer = Some("kyra"))

  private type ODAO = ObservationDAO[ObservationImpl]
  def run[R](fn: ODAO => R): R = Await.result(dao.runTransaction(fn), timeout)

  "ObservationDAOImpl" should "create" in {
    imagedMoment0.addObservation(observation0)
    run(_.create(observation0))
    observation0.uuid should not be null

    // -- Add a second
    imagedMoment0.addObservation(observation1)
    run(_.create(observation1))
  }

  it should "update" in {

    val duration = Duration.ofMillis(1234)
    run(d => {
      val obs = d.findByUUID(observation0.uuid)
      obs shouldBe defined
      obs.get.concept = newConcept
      obs.get.duration = duration
    })

    val obs = run(_.findByUUID(observation0.uuid)).head
    obs.concept should be(newConcept)
    obs.duration should be(duration)

  }

  it should "findByUUID" in {
    val obs = run(_.findByUUID(observation0.uuid))
    obs shouldBe defined
  }

  it should "findAll" in {
    val all = run(_.findAll())
    all.size should be >= 2
  }

  it should "findAllNames" in {
    val names = run(_.findAllNames())
    names should contain allOf (concept, newConcept)
  }

  it should "findAllNamesByVideoReferenceUUID" in {
    val names = run(_.findAllConceptsByVideoReferenceUUID(imagedMoment0.videoReferenceUUID))
    names should contain allOf (concept, newConcept)
  }

  it should "deleteByUUID" in {
    run(_.deleteByUUID(observation0.uuid))
    val obs = run(_.findByUUID(observation0.uuid))
    obs shouldBe empty
  }

  it should "delete" in {
    val obs = run(_.findAll()).filter(_.uuid == observation1.uuid)
    run(d => obs.foreach(d.delete))
    val obsCheck = run(_.findAll()).filter(_.uuid == observation1.uuid)
    obsCheck shouldBe empty
  }

  protected override def afterAll(): Unit = {
    H2TestDAOFactory.cleanup()
  }

}

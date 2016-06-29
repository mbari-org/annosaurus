package org.mbari.vars.annotation.dao.jpa

import java.time.{ Duration, Instant }
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.scalatest.{ FlatSpec, Matchers }

import scala.concurrent.{ Await, Awaitable }
import scala.concurrent.duration.{ Duration => SDuration }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-28T08:44:00
 */
class ObservationDAOSpec extends FlatSpec with Matchers {

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

  def run[R](fn: Awaitable[R]): R = Await.result(fn, timeout)

  "ObservationDAOImpl" should "create" in {
    imagedMoment0.addObservation(observation0)
    run(dao.runTransaction(d => d.create(observation0)))
    observation0.uuid should not be null

    // -- Add a second
    imagedMoment0.addObservation(observation1)
    run(dao.runTransaction(d => d.create(observation1)))
  }

  it should "update" in {

    val duration = Duration.ofMillis(1234)
    run(dao.runTransaction(d => {
      val obs = d.findByUUID(observation0.uuid)
      obs shouldBe defined
      obs.get.concept = newConcept
      obs.get.duration = duration
    }))

    val obs = run(dao.runTransaction(d => d.findByUUID(observation0.uuid))).head
    obs.concept should be(newConcept)
    obs.duration should be(duration)

  }

  it should "findByUUID" in {
    val obs = run(dao.runTransaction(d => d.findByUUID(observation0.uuid)))
    obs shouldBe defined
  }

  it should "findAll" in {
    val all = run(dao.runTransaction(d => d.findAll()))
    all.size should be >= 2
  }

  it should "findAllNames" in {
    val names = run(dao.runTransaction(d => d.findAllNames()))
    names should contain allOf (concept, newConcept)
  }

  it should "findAllNamesByVideoReferenceUUID" in {
    val names = run(dao.runTransaction(d => d.findAllNamesByVideoReferenceUUID(imagedMoment0.videoReferenceUUID)))
    names should contain allOf (concept, newConcept)
  }

  it should "deleteByUUID" in {
    run(dao.runTransaction(d => d.deleteByUUID(observation0.uuid)))
    val obs = run(dao.runTransaction(d => d.findByUUID(observation0.uuid)))
    obs shouldBe empty
  }

  it should "delete" in {
    val obs = run(dao.runTransaction(d => d.findAll())).filter(_.uuid == observation1.uuid)
    run(dao.runTransaction(d => obs.foreach(d.delete)))
    val obsCheck = run(dao.runTransaction(d => d.findAll())).filter(_.uuid == observation1.uuid)
    obsCheck shouldBe empty
  }

}

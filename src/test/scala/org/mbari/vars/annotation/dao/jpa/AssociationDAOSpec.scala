package org.mbari.vars.annotation.dao.jpa

import java.time.{ Duration, Instant }
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.model.Association
import org.scalatest.{ FlatSpec, Matchers }

import scala.concurrent.{ Await, Awaitable }
import scala.concurrent.duration.{ Duration => SDuration }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-28T09:49:00
 */
class AssociationDAOSpec extends FlatSpec with Matchers {

  private[this] val timeout = SDuration(2, TimeUnit.SECONDS)
  private[this] val imDao = H2TestDAOFactory.newImagedMomentDAO()
  private[this] val obsDao = H2TestDAOFactory.newObservationDAO(imDao)
  private[this] val dao = H2TestDAOFactory.newAssociationDAO(imDao)
  private[this] val videoReferenceUUID = UUID.randomUUID()
  private[this] val now = Instant.now()
  private[this] val imagedMoment0 = ImagedMomentImpl(Some(videoReferenceUUID), Some(now), elapsedTime = Some(Duration.ofMinutes(1)))
  private[this] val concept = "Grimpoteuthis"
  private[this] val observation0 = ObservationImpl(concept, observationDate = Some(now), observer = Some("brian"))
  private[this] val association0 = AssociationImpl("surface-color", Association.TO_CONCEPT_SELF, "red")
  private[this] val association1 = AssociationImpl("image-quality", Association.TO_CONCEPT_SELF, "mega-awesome!!")

  def run[R](fn: Awaitable[R]): R = Await.result(fn, timeout)

  "AssociationDAOImpl" should "create" in {
    imagedMoment0.addObservation(observation0)
    observation0.addAssociation(association0)
    run(dao.runTransaction(d => d.create(association0)))
    association0.uuid should not be null

    observation0.addAssociation(association1)
    run(dao.runTransaction(d => d.create(association0)))
  }

  it should "update" in {
    run(dao.runTransaction(d => {
      val ass = d.findByUUID(association0.uuid)
      ass shouldBe defined
      ass.get.linkValue = "blue"
    }))

    val ass = run(dao.runTransaction(d => d.findByUUID(association0.uuid))).head
    ass.linkValue should be("blue")
  }

  it should "findByUUID" in {
    val ass = run(dao.runTransaction(d => d.findByUUID(association0.uuid)))
    ass shouldBe defined
  }

  it should "findAll" in {
    val ass = run(dao.runTransaction(d => d.findAll()))
      .filter(_.observation.uuid == observation0.uuid)

    ass.size should be(2)
  }

  it should "findByLinkName" in {
    val ass = run(dao.runTransaction(d => d.findByLinkName("surface-color")))
    ass.size should be(1)
  }

  it should "deleteByUUID" in {
    run(dao.runTransaction(d => d.deleteByUUID(association0.uuid)))
    val obs = run(dao.runTransaction(d => d.findByUUID(association0.uuid)))
    obs shouldBe empty
  }

  it should "delete" in {
    val ass = run(dao.runTransaction(d => d.findAll()))
      .filter(_.uuid == association1.uuid)
    run(dao.runTransaction(d => ass.foreach(d.delete)))

    val assCheck = run(dao.runTransaction(d => d.findAll()))
      .filter(_.uuid == association1.uuid)
    assCheck shouldBe empty
  }

}

package org.mbari.vars.annotation.controllers

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.dao.jpa.{AnnotationImpl, AssociationImpl, ImagedMomentImpl, ObservationImpl, TestDAOFactory}
import org.mbari.vars.annotation.model.simple.ConceptAssociationRequest
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration => SDuration}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author Brian Schlining
 * @since 2019-06-05T14:52:00
 */
class AssociationControllerSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory = TestDAOFactory.Instance
  private[this] val controller = new AssociationController(daoFactory.asInstanceOf[BasicDAOFactory])
  private[this] val annotationController = new AnnotationController(daoFactory.asInstanceOf[BasicDAOFactory])
  private[this] val timeout = SDuration(200, TimeUnit.SECONDS)
  private[this] val recordedDate = Instant.now()

  def exec[R](fn: () => Future[R]): R = Await.result(fn.apply(), timeout)

  "AssociationController" should "find by ConceptAssociationRequest" in {

    // Create the annotations
    val uuids = 0 until 5 map(_ => UUID.randomUUID())
    val as = for {
      uuid <- uuids
      i <- 0 until 5
    } yield {
      val im = ImagedMomentImpl(recordedDate = Some(recordedDate), videoReferenceUUID = Some(uuid))
      val obs = ObservationImpl("Cyclops", observer = Some("brian"))
      im.addObservation(obs)
      val ass = AssociationImpl(s"foo-$i", "self", s"$i")
      obs.addAssociation(ass)
      AnnotationImpl(obs)
    }
    val annotations = exec(() => annotationController.bulkCreate(as))
    annotations.size should be (25)

    // Find by request
    val linkName = "foo-0"
    val request = ConceptAssociationRequest(linkName, uuids)
    val response = exec(() => controller.findByConceptAssociationRequest(request))
//    println(Constants.GSON.toJson(response))
    response.conceptAssociationRequest.linkName should be (linkName)
    response.conceptAssociationRequest.uuids should contain theSameElementsAs uuids
    response.associations.size should be (5)
    response.associations.foreach(a => a.linkName should be (linkName))
    response.associations.foreach(a => a.linkValue should be ("0"))

  }

  protected override def afterAll(): Unit = {
    daoFactory.cleanup()
  }
}

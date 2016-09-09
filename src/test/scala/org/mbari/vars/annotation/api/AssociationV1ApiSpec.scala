package org.mbari.vars.annotation.api

import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.controllers.{ AnnotationController, AssociationController }
import org.mbari.vars.annotation.dao.jpa.AssociationImpl
import org.mbari.vars.annotation.model.Association
import org.mbari.vars.annotation.model.simple.Annotation

import scala.concurrent.Await
import scala.concurrent.duration.{ Duration => SDuration }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-09-08T16:48:00
 */
class AssociationV1ApiSpec extends WebApiStack {

  private[this] val associationV1Api = {
    val controller = new AssociationController(daoFactory)
    new AssociationV1Api(controller)
  }

  addServlet(associationV1Api, "/v1/associations")

  var annotation: Annotation = _
  var association: Association = _

  "AssociationV1Api" should "create" in {

    annotation = {
      val controller = new AnnotationController(daoFactory)
      Await.result(
        controller.create(UUID.randomUUID(), "Foo", "brian",
          elapsedTime = Some(Duration.ofMillis(2000))),
        SDuration(3000, TimeUnit.MILLISECONDS)
      )
    }

    post(
      s"/v1/associations/",
      "observation_uuid" -> annotation.observationUuid.toString,
      "link_name" -> "color",
      "link_value" -> "red"
    ) {
        status should be(200)
        association = gson.fromJson(body, classOf[AssociationImpl])
        association.linkName should be("color")
        association.linkValue should be("red")
      }
  }

  it should "find by uuid" in {

  }

  it should "find by videoreference.uuid and linkName" in {

  }

  it should "update" in {

  }

  it should "delete" in {

  }

}

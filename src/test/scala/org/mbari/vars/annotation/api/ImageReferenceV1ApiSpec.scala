package org.mbari.vars.annotation.api

import java.net.URL
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.controllers.{ ImageReferenceController, ImagedMomentController }
import org.mbari.vars.annotation.dao.jpa.{ ImageReferenceImpl, ImagedMomentImpl }

import scala.concurrent.Await
import scala.concurrent.duration.{ Duration => SDuration }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-09-09T10:18:00
 */
class ImageReferenceV1ApiSpec extends WebApiStack {

  private[this] val timeout = SDuration(3000, TimeUnit.MILLISECONDS)

  private[this] val imageReferenceV1Api = {
    val controller = new ImageReferenceController(daoFactory)
    new ImageReferenceV1Api(controller)
  }

  private[this] val path = "/v1/imagereferences"
  addServlet(imageReferenceV1Api, path)

  var imageReference: ImageReferenceImpl = _

  "ImageReferenceV1Spec" should "find by uuid" in {

    // --- Create an imageref
    val dao = daoFactory.newImageReferenceDAO()
    val imageMoment = ImagedMomentImpl(Some(UUID.randomUUID()), Some(Instant.now()))
    imageReference = ImageReferenceImpl(new URL("http://www.mbari.org/foo.png"), format = Some("image/png"))
    imageMoment.addImageReference(imageReference)
    val f = dao.runTransaction(d => d.create(imageReference))
    f.onComplete(t => dao.close())
    Await.result(f, timeout)

    // --- find it via the web api
    get(s"$path/${imageReference.uuid}") {
      status should be(200)
      val ir = gson.fromJson(body, classOf[ImageReferenceImpl])
      ir.uuid should be(imageReference.uuid)
      ir.url should be(imageReference.url)
    }

  }

  it should "update" in {
    put(
      s"$path/${imageReference.uuid}",
      "url" -> "http://www.google.com/bar.jpg",
      "format" -> "image/jpg",
      "width_pixels" -> "1920",
      "height_pixels" -> "1080",
      "description" -> "updated"
    ) {
        status should be(200)
        val ir = gson.fromJson(body, classOf[ImageReferenceImpl])
        ir.uuid should be(imageReference.uuid)
        ir.url should be(new URL("http://www.google.com/bar.jpg"))
        ir.width should be(1920)
        ir.height should be(1080)
        ir.description should be("updated")
      }
  }

  it should "update (move to new imagedmoment)" in {

    // --- Create a new imagedmoment and insert
    val dao = daoFactory.newImagedMomentDAO()
    val imageMoment = ImagedMomentImpl(Some(UUID.randomUUID()), Some(Instant.now()))
    val f = dao.runTransaction(d => d.create(imageMoment))
    f.onComplete(t => dao.close())
    Await.result(f, timeout)

    // --- Move imagereference to a new imagedmoment
    put(s"$path/${imageReference.uuid}", "imaged_moment_uuid" -> imageMoment.uuid.toString) {
      status should be(200)
      val ir = gson.fromJson(body, classOf[ImageReferenceImpl])
      ir.uuid should be(imageReference.uuid)
    }

    // --- Verify the move
    val controller = new ImagedMomentController(daoFactory)
    val f2 = controller.findByImageReferenceUUID(imageReference.uuid)
    val imagedMoment = Await.result(f2, timeout)
    imagedMoment should not be empty
    val im = imagedMoment.get
    im.imageReferences.map(_.uuid) should contain(imageReference.uuid)

  }

  it should "delete" in {
    delete(s"$path/${imageReference.uuid}") {
      status should be(204)
    }
    get(s"$path/${imageReference.uuid}") {
      status should be(404)
    }
  }

}

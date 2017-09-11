package org.mbari.vars.annotation.api

import java.net.{ URL, URLEncoder }
import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.controllers.ImageController
import org.mbari.vars.annotation.model.simple.Image

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-09-12T14:38:00
 */
class ImageV1ApiSpec extends WebApiStack {

  private[this] val imageV1Api = {
    val controller = new ImageController(daoFactory)
    new ImageV1Api(controller)
  }

  val path = "/v1/images"
  addServlet(imageV1Api, "/v1/images")

  var image: Image = _

  "ImageV1Api" should "create" in {
    post(
      path,
      "video_reference_uuid" -> UUID.randomUUID().toString,
      "elapsed_time_millis" -> "12345",
      "format" -> "image/jpg",
      "url" -> "http://www.mbari.org/foo.jpg",
      "width_pixels" -> "1920",
      "height_pixels" -> "1080",
      "description" -> "This is a test"
    ) {
        status should be(200)
        image = gson.fromJson(body, classOf[Image])
        image.url should be(new URL("http://www.mbari.org/foo.jpg"))
        image.format should be("image/jpg")
        image.width should be(1920)
        image.height should be(1080)
        image.description should be("This is a test")
        image.elapsedTime should be(Duration.ofMillis(12345))
      }
  }

  it should "create with same index" in {
    post(
      path,
      "video_reference_uuid" -> image.videoReferenceUuid.toString,
      "elapsed_time_millis" -> "12345",
      "format" -> "image/png",
      "url" -> "http://www.mbari.org/foo.png"
    ) {
        status should be(200)
        val im = gson.fromJson(body, classOf[Image])
        image.imagedMomentUuid should be(im.imagedMomentUuid)
      }
  }

  it should "find by videoreference" in {
    get(s"$path/videoreference/${image.videoReferenceUuid}") {
      status should be(200)
      val images = gson.fromJson(body, classOf[Array[Image]])
      images.size should be(2)
    }
  }

  it should "find by uuid" in {
    get(s"$path/${image.imageReferenceUuid}") {
      status should be(200)
      val im = gson.fromJson(body, classOf[Image])
      im.imagedMomentUuid should be(image.imagedMomentUuid)
      im.url should be(image.url)
    }
  }

  it should "find by url" in {
    val url = URLEncoder.encode(image.url.toExternalForm, "UTF-8")
    get(s"$path/url/${url}") {
      status should be(200)
      val im = gson.fromJson(body, classOf[Image])
      im.url should be(new URL("http://www.mbari.org/foo.jpg"))
    }
  }

  it should "update" in {
    put(
      s"$path/${image.imageReferenceUuid}",
      "url" -> "http://www.google.com/bar.jpg",
      "elapsed_time_millis" -> "20345",
      "recorded_timestamp" -> "1968-09-22T02:00:55Z",
      "width_pixels" -> "4000",
      "height_pixels" -> "2000",
      "video_reference_uuid" -> UUID.randomUUID().toString
    ) {
        status should be(200)
        val im = gson.fromJson(body, classOf[Image])
        im.url should be(new URL("http://www.google.com/bar.jpg"))
        im.elapsedTime should be(Duration.ofMillis(20345))
        im.recordedTimestamp should be(Instant.parse("1968-09-22T02:00:55Z"))
        im.width should be(4000)
        im.height should be(2000)
        im.videoReferenceUuid should not be (image.videoReferenceUuid)
      }
  }

}

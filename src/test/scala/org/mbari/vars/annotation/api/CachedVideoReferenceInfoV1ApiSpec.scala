package org.mbari.vars.annotation.api

import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.Constants
import org.mbari.vars.annotation.controllers.CachedVideoReferenceInfoController
import org.mbari.vars.annotation.dao.jpa.CachedVideoReferenceInfoImpl
import org.mbari.vars.annotation.model.CachedVideoReferenceInfo

import scala.concurrent.duration.{ Duration => SDuration }
import scala.collection.mutable

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-09-14T15:55:00
 */
class CachedVideoReferenceInfoV1ApiSpec extends WebApiStack {

  private[this] val timeout = SDuration(3000, TimeUnit.MILLISECONDS)
  protected[this] override val gson = Constants.GSON

  private[this] val videoinfoV1Api = {
    val controller = new CachedVideoReferenceInfoController(daoFactory)
    new CachedVideoReferenceInfoV1Api(controller)
  }

  private[this] val path = "/v1/videoinfos"

  addServlet(videoinfoV1Api, path)

  var videoinfos = new mutable.ArrayBuffer[CachedVideoReferenceInfo]

  val n = 10

  "CachedVideoReferenceInfoApi" should "create" in {

    for (i <- 0 until n) {
      post(
        s"$path",
        "video_reference_uuid" -> UUID.randomUUID().toString,
        "mission_contact" -> "brian",
        "mission_id" -> i.toString,
        "platform_name" -> s"Ventana_$i"
      ) {

          status should be(200)
          val vi = gson.fromJson(body, classOf[CachedVideoReferenceInfoImpl])
          vi.videoReferenceUUID should not be (null)
          vi.missionContact should be("brian")
          vi.missionID should be(i.toString)
          vi.platformName should be(s"Ventana_$i")
          videoinfos += vi
        }
    }
  }

  it should "find all" in {
    get(path + "/") {
      status should be(200)
      val vis = gson.fromJson(body, classOf[Array[CachedVideoReferenceInfoImpl]])
      vis.size should be(n)
      vis.map(_.platformName) should contain theSameElementsAs videoinfos.map(_.platformName)
      vis.map(_.missionID) should contain theSameElementsAs videoinfos.map(_.missionID)
      vis.map(_.videoReferenceUUID) should contain theSameElementsAs videoinfos.map(_.videoReferenceUUID)
      vis.map(_.missionContact) should contain theSameElementsAs videoinfos.map(_.missionContact)
    }
  }

  it should "update" in {
    var vis: Array[CachedVideoReferenceInfoImpl] = Array.empty
    get(path + "/") {
      status should be(200)
      vis = gson.fromJson(body, classOf[Array[CachedVideoReferenceInfoImpl]])
      vis.size should be(n)
    }

    for (i <- 0 until n) {
      val v = vis(i)
      post(
        s"$path/${v.uuid}",
        "mission_contact" -> "schlin", "mission_id" -> ("xxx" + i), "platform_name" -> "Doc Ricketts"
      ) {
          status should be(200)
          val v2 = gson.fromJson(body, classOf[CachedVideoReferenceInfoImpl])
          v2.videoReferenceUUID should be(v.videoReferenceUUID)
          v2.missionContact should be("schlin")
          v2.missionID should be(s"xxx$i")
          v2.platformName should be("Doc Ricketts")
        }
    }
  }

  it should "delete" in {
    for (v <- videoinfos) {
      delete(s"$path/${v.uuid}") {
        status should be(204)
      }
    }
  }

}

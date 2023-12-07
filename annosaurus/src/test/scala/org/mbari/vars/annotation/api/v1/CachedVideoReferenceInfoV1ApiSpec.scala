/*
 * Copyright 2017 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.vars.annotation.api.v1

import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.Constants
import org.mbari.vars.annotation.api.WebApiStack
import org.mbari.vars.annotation.controllers.CachedVideoReferenceInfoController
import org.mbari.vars.annotation.repository.jpa.CachedVideoReferenceInfoEntity
import org.mbari.vars.annotation.model.CachedVideoReferenceInfo

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.{Duration => SDuration}

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-09-14T15:55:00
  */
class CachedVideoReferenceInfoV1ApiSpec extends WebApiStack {

  private[this] val timeout         = SDuration(3000, TimeUnit.MILLISECONDS)
  override protected[this] val gson = Constants.GSON

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
        "mission_contact"      -> "brian",
        "mission_id"           -> i.toString,
        "platform_name"        -> s"Ventana_$i"
      ) {

        status should be(200)
        val vi = gson.fromJson(body, classOf[CachedVideoReferenceInfoEntity])
        vi.videoReferenceUUID should not be (null)
        vi.missionContact should be("brian")
        vi.missionId should be(i.toString)
        vi.platformName should be(s"Ventana_$i")
        videoinfos += vi
      }
    }
  }

  it should "find all" in {
    get(path + "/") {
      status should be(200)
      val vis = gson.fromJson(body, classOf[Array[CachedVideoReferenceInfoEntity]])
      vis.size should be(n)
      vis.map(_.platformName) should contain theSameElementsAs videoinfos.map(_.platformName)
      vis.map(_.missionId) should contain theSameElementsAs videoinfos.map(_.missionId)
      vis.map(_.videoReferenceUUID) should contain theSameElementsAs videoinfos.map(
        _.videoReferenceUUID
      )
      vis.map(_.missionContact) should contain theSameElementsAs videoinfos.map(_.missionContact)
    }
  }

  it should "update" in {
    var vis: Array[CachedVideoReferenceInfoEntity] = Array.empty
    get(path + "/") {
      status should be(200)
      vis = gson.fromJson(body, classOf[Array[CachedVideoReferenceInfoEntity]])
      vis.size should be(n)
    }

    for (i <- 0 until n) {
      val v = vis(i)
      put(
        s"$path/${v.uuid}",
        "mission_contact" -> "schlin",
        "mission_id"      -> ("xxx" + i),
        "platform_name"   -> "Doc Ricketts"
      ) {
        status should be(200)
        val v2 = gson.fromJson(body, classOf[CachedVideoReferenceInfoEntity])
        v2.videoReferenceUUID should be(v.videoReferenceUUID)
        v2.missionContact should be("schlin")
        v2.missionId should be(s"xxx$i")
        v2.platformName should be("Doc Ricketts")

        get(s"$path/${v.uuid}") {
          status should be(200)
          val v3 = gson.fromJson(body, classOf[CachedVideoReferenceInfoEntity])
          v3.videoReferenceUUID should be(v2.videoReferenceUUID)
          v3.missionContact should be(v2.missionContact)
          v3.missionId should be(v2.missionId)
          v3.platformName should be(v2.platformName)
        }
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

  override protected def afterAll(): Unit = {
    super.afterAll()
    val dao = daoFactory.newCachedVideoReferenceInfoDAO()

    val f = dao.runTransaction(d => {
      val all = dao.findAll()
      all.foreach(dao.delete)
    })
    f.onComplete(t => dao.close())
    Await.result(f, SDuration(4, TimeUnit.SECONDS))

    super.afterAll()
  }

}

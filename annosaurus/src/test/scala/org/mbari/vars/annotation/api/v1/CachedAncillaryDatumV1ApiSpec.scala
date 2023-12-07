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

import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.UUID

import org.mbari.vars.annotation.Constants
import org.mbari.vars.annotation.api.WebApiStack
import org.mbari.vars.annotation.controllers.CachedAncillaryDatumController
import org.mbari.vars.annotation.model.simple.CachedAncillaryDatumBean
import org.mbari.vars.annotation.repository.jpa.entity.CachedAncillaryDatumEntity

import scala.collection.JavaConverters._

/**
  *
  *
  * @author Brian Schlining
  * @since 2017-11-13T16:26:00
  */
class CachedAncillaryDatumV1ApiSpec extends WebApiStack {

  private[this] val datumV1Api = {
    val controller = new CachedAncillaryDatumController(daoFactory)
    new CachedAncillaryDatumV1Api(controller)
  }

  private[this] val path = "/v1/ancillarydata"
  addServlet(datumV1Api, path)

  private[this] val imagedMoments = {
    val videoReferenceUuid = UUID.randomUUID()
    val dao                = daoFactory.newImagedMomentDAO()
    val ims = (0 until 10).map(i =>
      dao.newPersistentObject(
        videoReferenceUuid,
        elapsedTime = Some(Duration.ofMillis(math.round(math.random() * 10000L)))
      )
    )
    dao.runTransaction(d => {
      ims.foreach(dao.create)
    })
    ims
  }

  "CachedAncillaryDatumV1Api" should "create, then update" in {
    val data = imagedMoments.map(im => {
      val i: Float = (math.random() * 10f).floatValue()
      val d        = new CachedAncillaryDatumBean
      d.imagedMomentUuid = im.uuid
      d.latitude = Some(36 + i)
      d.longitude = Some(-122 + i)
      d.depthMeters = Some(100f * i)
      d.oxygenMlL = Some(0.23f * i)
      d.salinity = Some(35 + i)
      d
    })
    val json = Constants.GSON.toJson(data.asJava)

    // --- Create
    post(
      s"$path/bulk",
      headers = Map("Content-Type" -> "application/json"),
      body = json.getBytes(StandardCharsets.UTF_8)
    ) {
      status should be(200)

      // Note that if bulid create is successful it just sends back
      // the exact same data you sent. So there will be no lastUpdated
      // or uuid values set.
      val persistedData = Constants
        .GSON
        .fromJson(body, classOf[Array[CachedAncillaryDatumEntity]])
        .toSeq
      persistedData.size should be(imagedMoments.size)
      persistedData
        .indices
        .foreach(i => {
          val a = data(i)
          val b = persistedData(i)

          b.uuid === null
          b.latitude.get should be(a.latitude.get)
          b.longitude.get should be(a.longitude.get)
          b.depthMeters.get should be(a.depthMeters.get)
          b.oxygenMlL.get should be(a.oxygenMlL.get)
          b.salinity.get should be(a.salinity.get)
          b.temperatureCelsius should be(None)
        })
    }

    // --- Update
    data.foreach(d => {
      d.oxygenMlL = None
      d.salinity = Some(14)
    })

    val json2 = Constants.GSON.toJson(data.asJava)
    post(
      s"$path/bulk",
      headers = Map("Content-Type" -> "application/json"),
      body = json2.getBytes(StandardCharsets.UTF_8)
    ) {
      status should be(200)
      val persistedData = Constants
        .GSON
        .fromJson(body, classOf[Array[CachedAncillaryDatumEntity]])
        .toSeq
//        println(body)
      persistedData.size should be(imagedMoments.size)
      persistedData
        .indices
        .foreach(i => {
          val a = data(i)
          val b = persistedData(i)

          b.uuid === null
          b.latitude.get should be(a.latitude.get)
          b.longitude.get should be(a.longitude.get)
          b.depthMeters.get should be(a.depthMeters.get)
          b.oxygenMlL should be(None)
          b.salinity.get should be(14)
          b.temperatureCelsius should be(None)
        })
    }

  }

}

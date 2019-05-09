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
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.api.WebApiStack
import org.mbari.vars.annotation.controllers.{AnnotationController, IndexController}
import org.mbari.vars.annotation.dao.jpa.IndexImpl
import org.mbari.vcr4j.time.{FrameRates, Timecode}

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.{Duration => SDuration}

/**
 * @author Brian Schlining
 * @since 2019-02-08T14:19:00
 */
class IndexV1ApiSpec extends WebApiStack {

  private[this] val startTimestamp = Instant.now()

  private[this] val indexController = new IndexController(daoFactory)

  private[this] val indexV1Api = new IndexV1Api(indexController)

  private[this] val annotationController = new AnnotationController(daoFactory)

  private[this] val timeout = SDuration(3000, TimeUnit.MILLISECONDS)

  addServlet(indexV1Api, "/v1/index")

  private[this] val videoReferenceUuid = UUID.randomUUID()

  //  protected override def beforeAll(): Unit = {
  //    // Create data
  ////    for (i <- 0 until 10) {
  ////      Await.result(
  ////        annotationController.create(videoReferenceUuid, "Foo", "brian",
  ////          elapsedTime = Some(Duration.ofMillis(10000 * i))),
  ////        SDuration(3000, TimeUnit.MILLISECONDS))
  ////    }
  //  }

  "IndexV1Api" should "find by videoreferenceuuid" in {

    // Create data
    for (i <- 0 until 10) {
      Await.result(
        annotationController.create(
          videoReferenceUuid,
          "Foo",
          "brian",
          timecode = Some(new Timecode(i * 100, FrameRates.NTSC))),
        timeout)
    }

    get(s"/v1/index/videoreference/$videoReferenceUuid") {
      status should be(200)
      val ids = gson.fromJson(body, classOf[Array[IndexImpl]])
      ids.size should be(10)
      println(body)
    }

  }

  it should "bulkUpdateRecordedTimestamp" in {

    val now = Instant.now()
    val indices = Await.result(indexController.findByVideoReferenceUUID(videoReferenceUuid), timeout)
    indices.foreach(_.recordedDate = now)
    val json = gson.toJson(indices.asJava)

    put(
      "/v1/index/tapetime",
      headers = Map("Content-Type" -> "application/json"),
      body = json.getBytes(StandardCharsets.UTF_8)) {

        status should be(200)

        val newIndices = gson.fromJson(body, classOf[Array[IndexImpl]])
        newIndices.size should be(10)
        for (i <- newIndices) {
          i.recordedDate should not be null
          i.timecode should not be null
          i.recordedDate should be(now)
        }

      }
  }

}

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

package org.mbari.annosaurus.api.v2

import org.mbari.annosaurus.api.WebApiStack
import org.mbari.annosaurus.controllers.AnnotationController
import org.mbari.annosaurus.repository.jpa.MutableAnnotationImpl

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class AnnotationV2ApiSpec extends WebApiStack {

  private[this] val controller         = new AnnotationController(daoFactory)
  private[this] val annotationV2Api    = new AnnotationV2Api(controller)
  private[this] val videoReferenceUuid = UUID.randomUUID()
  private[this] val startTimestamp     = Instant.parse("2000-01-01T00:00:00Z")
  private[this] val endTimestamp       = Instant.parse("2000-02-01T00:00:00Z")

  addServlet(annotationV2Api, "/v2/annotations")

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val timeout = Duration(5, TimeUnit.SECONDS)

    Await.result(
      controller.create(videoReferenceUuid, "one", "brian", recordedDate = Some(startTimestamp)),
      timeout
    )
    Await.result(
      controller.create(videoReferenceUuid, "two", "brian", recordedDate = Some(endTimestamp)),
      timeout
    )
    Await.result(
      controller.create(
        videoReferenceUuid,
        "three",
        "brian",
        recordedDate = Some(Instant.parse("2019-01-01T00:00:00Z"))
      ),
      timeout
    )
  }

  "AnnotationV2Api" should "find by videoReferenceUuid" in {
    get(s"/v2/annotations/videoreference/${videoReferenceUuid}") {
      status should be(200)
      val xs = gson.fromJson(body, classOf[Array[MutableAnnotationImpl]])
      xs.size should be(3)
    }
  }

  it should "find by videoReferenceUuid and timestamps" in {
    get(
      s"/v2/annotations/videoreference/${videoReferenceUuid}?start=20000101T000000Z&end=20000201T000000Z"
    ) {
      status should be(200)
      val xs = gson.fromJson(body, classOf[Array[MutableAnnotationImpl]])
      xs.size should be(2)
    }
  }
}

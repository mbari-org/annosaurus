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

package org.mbari.vars.annotation.dao.jpa

import java.net.URL
import java.time.Instant
import java.util.UUID

import org.mbari.vars.annotation.Constants
import org.scalatest.{ FlatSpec, Matchers }

import scala.io.Source

/**
 * @author Brian Schlining
 * @since 2017-09-20T14:59:00
 */
class GSONTest extends FlatSpec with Matchers {

  "GSON" should "convert to an annotation with image references" in {
    val src = getClass.getResource("/json/annotation_single.json")
    val json = Source.fromFile(src.toURI, "UTF-8").mkString
    val annotations = Constants.GSON.fromJson(json, classOf[Array[AnnotationImpl]])

    annotations.size should be(1)
    annotations.head.imageReferences.size should be(2)
    annotations.head.imageReferences.head.url should not be null
  }

  it should "round trip annotation json" in {
    val annotation = AnnotationImpl(UUID.randomUUID(), "Foo", "brian",
      recordedDate = Option(Instant.now))
    val imageReference = ImageReferenceImpl(new URL("http://www.bob.com/uncle.jpg"))
    annotation.imageReferences = Seq(imageReference)
    val json = Constants.GSON.toJson(annotation)
    val annos = Constants.GSON.fromJson(json, classOf[AnnotationImpl])
    annos.imageReferences.size should be(1)
    annos.imageReferences.head.url should not be null
  }

  it should "round trip an imaged moment to/from json" in {
    val imagedMoment = ImagedMomentImpl(Option(UUID.randomUUID()), Option(Instant.now()))
    val imageReference = ImageReferenceImpl(new URL("http://www.boo.org/booya.png"))
    val observation = ObservationImpl("Nanomia", observer = Some("brian"))
    val association = AssociationImpl("eating", "trump", "nil")
    imagedMoment.addImageReference(imageReference)
    imagedMoment.addObservation(observation)
    observation.addAssociation(association)

    val json = Constants.GSON.toJson(imagedMoment)
    val im0 = Constants.GSON.fromJson(json, classOf[ImagedMomentImpl])
    im0 should not be null
    im0.observations.size should be(1)
    im0.observations.head.concept should be(observation.concept)
    im0.observations.head.associations.size should be(1)
    im0.observations.head.associations.head.linkName should be(association.linkName)
    im0.imageReferences.size should be(1)
    im0.imageReferences.head.url should be(imageReference.url)
  }

  it should "round trip an ancillary datum to/from json" in {
    val datum = CachedAncillaryDatumImpl(36, -122, 1000, 35.234F, 13F, 1003F, 3.4F)
    val json = Constants.GSON.toJson(datum)
    val datum0 = Constants.GSON.fromJson(json, classOf[CachedAncillaryDatumImpl])
    datum0.latitude should not be None
    datum0.latitude.get should be(datum.latitude.get)
    datum0.longitude.get should be(datum.longitude.get)
    datum0.depthMeters.get should be(datum.depthMeters.get)
    datum0.salinity.get should be(datum.salinity.get +- 0.0001F)
  }

}

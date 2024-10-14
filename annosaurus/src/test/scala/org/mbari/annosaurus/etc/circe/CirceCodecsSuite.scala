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

package org.mbari.annosaurus.etc.circe

import scala.io.Source
import scala.util.Using
import org.mbari.annosaurus.domain.{ImagedMoment, ImagedMomentSC, ObservationsUpdate, QueryConstraints}
import CirceCodecs.{*, given}
import org.mbari.annosaurus.repository.query.Constraints

import scala.util.Failure
import scala.util.Success
import java.time.Instant
import java.util.UUID

class CirceCodecsSuite extends munit.FunSuite {

    /* 
      Test to decode mutliple imaged moments from snake case json to camel case object.
      We check ALL of the values to make sure they are decoded correctly.
    */
    test("decode imagedMoments") {
        val url = getClass.getResource("/json/imaged_moments.json")
        assert(url != null)
        val t   = Using(Source.fromURL(url)) { source =>
            val json = source.getLines().mkString
            assert(json != null)
            json.reify[List[ImagedMoment]] match
                case Left(value)          => fail("Failed to pares imaged_moments.json. " + value.getMessage)
                case Right(imagedMoments) =>
                    assertEquals(imagedMoments.size, 10)

                    val im = imagedMoments.filter(i => i.recordedTimestamp.isDefined)
                        .minBy(_.recordedTimestamp)
                    assertEquals(im.timecode.get, "00:34:03:16")
                    assertEquals(im.recordedTimestamp.get.toString, "2007-05-25T16:02:38Z")
                    assertEquals(
                        im.videoReferenceUuid.toString,
                        "e3005de7-2c0a-4a3f-80ad-79e5e79ee4b8"
                    )
                    assertEquals(im.lastUpdated, Some(Instant.parse("2019-02-11T13:00:50Z")))
                    assertEquals(im.uuid.get.toString, "7591f5a7-5d4a-4f18-9377-0000051d205f")
                    assertEquals(im.imageReferences.size, 1)
                    assertEquals(im.observations.size, 1)
                    assert(im.ancillaryData.isDefined)

                    val i = im.imageReferences.head
                    assertEquals(i.description.get, "compressed image with overlay")
                    assertEquals(i.url.toString, "http://search.mbari.org/ARCHIVE/frameGrabs/Doc%20Ricketts/images/0616/01_17_59_10.jpg")
                    assertEquals(i.widthPixels.get, 1920)
                    assertEquals(i.heightPixels.get, 1080)
                    assertEquals(i.format.get, "image/jpg")
                    assertEquals(i.lastUpdated, Some(Instant.parse("2018-12-31T12:04:45Z")))
                    assertEquals(i.uuid.get.toString, "2bb028ae-8698-4332-9c20-41f9bf985d26")

                    val o = im.observations.head
                    assertEquals(o.concept, "Euphausiacea")
                    assertEquals(o.observationTimestamp.get.toString, "2009-03-16T22:49:36.530Z")
                    assertEquals(o.observer.get, "svonthun")
                    assertEquals(o.group.get, "ROV")
                    assertEquals(o.activity.get, "stationary")
                    assertEquals(o.uuid.get.toString, "8649142a-6f69-472b-984c-310be35d7e83")
                    assertEquals(o.associations.size, 1)
                    assertEquals(o.lastUpdated, Some(Instant.parse("2019-01-09T08:30:34Z")))

                    val a = o.associations.head
                    assertEquals(a.linkName, "population-quantity")
                    assertEquals(a.toConcept, "self")
                    assertEquals(a.linkValue, "999")
                    assertEquals(a.mimeType, Some("text/plain"))
                    assertEquals(a.lastUpdated, Some(Instant.parse("2019-01-09T08:30:34Z")))

                    val d = im.ancillaryData.get
                    assertEqualsDouble(d.oxygenMlL.get, 1.5099999904632568, 0.00001)
                    assertEqualsDouble(d.depthMeters.get, 91.48985290527344, 0.00001)
                    assertEqualsDouble(d.latitude.get, 36.75586, 0.000001)
                    assertEqualsDouble(d.temperatureCelsius.get, 8.442999839782715, 0.00001)
                    assertEqualsDouble(d.theta.get, -3.5, 0.1)
                    assertEqualsDouble(d.longitude.get, -121.913426, 0.000001)
                    assertEqualsDouble(d.phi.get, 0.10000000149011612, 0.00001)
                    assertEqualsDouble(d.psi.get, 175, 0.1)
                    assertEqualsDouble(d.pressureDbar.get, 91.19999694824219, 0.00001)
                    assertEqualsDouble(d.salinity.get, 34.14099884033203, 0.00001)
                    assertEqualsDouble(d.lightTransmission.get, 85.86000061035156, 0.00001)
                    assertEquals(d.lastUpdated, Some(Instant.parse("2023-08-20T16:22:59Z")))
                    assertEquals(d.uuid.get.toString, "286cf79a-b831-48cd-b94b-212bc10d16f4")
        }

        t match
            case Failure(e) =>
                e.printStackTrace()
                fail("Failed to read imaged_moments.json. " + e.getMessage())
            case Success(_) => ()

    }

    /* 
        Test to round trip json to CamelCase. Steps are:
            1. Decode from json file
            2. Encode back to our own json
            3. Decode our json back to an object
            4. Compare the original object to the decoded object
     */
    test("decode/encode ImagedMoment") {
        val url = getClass.getResource("/json/imaged_moment_complete.json")
        assert(url != null)
        val t   = Using(Source.fromURL(url)) { source =>
            val json = source.getLines().mkString
            assert(json != null)
            json.reify[ImagedMoment] match {
                case Left(value)  => fail("Failed to pares imaged_moment.json. " + value.getMessage)
                case Right(im0) =>
                    val json0 = im0.stringify
                    val opt1 = json0.reify[ImagedMoment].toOption
                    assert(opt1.isDefined)
                    val im1 = opt1.get
                    assertEquals(im0, im1)
            }
        }

        t match
            case Failure(e) =>
                e.printStackTrace()
                fail("Failed to read imaged_moments.json. " + e.getMessage())
            case Success(_) => ()

    }

    /* 
        Test to round trip json to snake_case. Steps are:
            1. Decode from json file
            2. Encode back to our own json
            3. Decode our json back to an object
            4. Compare the original object to the decoded object
     */
    test("decode/encode ImagedMomentSC") {
        val url = getClass.getResource("/json/imaged_moment_complete.json")
        assert(url != null)
        val t   = Using(Source.fromURL(url)) { source =>
            val json = source.getLines().mkString
            assert(json != null)
            json.reify[ImagedMomentSC] match {
                case Left(value)  => fail("Failed to pares imaged_moment.json. " + value.getMessage)
                case Right(im0) =>
                    val json0 = im0.stringify
                    val opt1 = json0.reify[ImagedMomentSC].toOption
                    assert(opt1.isDefined)
                    val im1 = opt1.get
                    assertEquals(im0, im1)
            }
        }

        t match
            case Failure(e) =>
                e.printStackTrace()
                fail("Failed to read imaged_moments.json. " + e.getMessage())
            case Success(_) => ()

    }

    /* 
        Decode snake case json to both snake case and camel case objects. Then compare the two objects.
     */
    test("snake_case vs camelCase") {
        val url = getClass.getResource("/json/imaged_moment_complete.json")
        assert(url != null)
        val t   = Using(Source.fromURL(url)) { source =>
            val json = source.getLines().mkString
            assert(json != null)
            json.reify[ImagedMomentSC] match {
                case Left(value)  => fail("Failed to pares imaged_moment.json. " + value.getMessage)
                case Right(im0) =>
                    val json0 = im0.stringify
                    json0.reify[ImagedMoment] match
                        case Left(value)  => fail("Failed to parse circe generated json. " + value.getMessage)
                        case Right(im1) =>
                            assertEquals(im0.toCamelCase, im1)

            }
        }
    }

    test("round trip Option[Double]") {
        val a = Some(1.0)
        val b = a.stringify.reify[Option[Double]].toOption.flatten
        assertEquals(a, b)

        val c = None
        val d = c.stringify.reify[Option[Double]].toOption.flatten
        assertEquals(c, d)
    }

    test("decode QueryConstraints") {
        val json = """{
                     |    "videoReferenceUuids": [],
                     |    "concepts": [
                     |        "Grimpoteuthis",
                     |        "Grimpoteuthis bathynectes",
                     |        "Grimpoteuthis sp. 1",
                     |        "Grimpoteuthis sp. 4",
                     |        "Grimpoteuthis sp. 5",
                     |        "Grimpoteuthis tuftsi"
                     |    ],
                     |    "observers": [],
                     |    "groups": [],
                     |    "activities": [],
                     |    "missionContacts": [],
                     |    "limit": 5000,
                     |    "data": true
                     |}""".stripMargin
        val qc = json.reify[QueryConstraints].toOption
        assert(qc.isDefined)
        assertEquals(qc.get.videoReferenceUuids, List.empty)
        assertEquals(qc.get.concepts, List("Grimpoteuthis", "Grimpoteuthis bathynectes", "Grimpoteuthis sp. 1", "Grimpoteuthis sp. 4", "Grimpoteuthis sp. 5", "Grimpoteuthis tuftsi"))
    }

    test("decode QueryConstraints from snake_case") {
        val json =
            """{
              |    "video_reference_uuids": [],
              |    "concepts": [
              |        "Grimpoteuthis",
              |        "Grimpoteuthis bathynectes",
              |        "Grimpoteuthis sp. 1",
              |        "Grimpoteuthis sp. 4",
              |        "Grimpoteuthis sp. 5",
              |        "Grimpoteuthis tuftsi"
              |    ],
              |    "observers": [],
              |    "groups": [],
              |    "activities": [],
              |    "mission_contacts": [],
              |    "limit": 5000,
              |    "data": true
              |}""".stripMargin
        val opt = json.reify[QueryConstraints].toOption
        assert(opt.isDefined)
        val qc = opt.get
        assertEquals(qc.videoReferenceUuids, List.empty)
        assert(qc.missionContacts.isEmpty)
        assertEquals(opt.get.concepts, List("Grimpoteuthis", "Grimpoteuthis bathynectes", "Grimpoteuthis sp. 1", "Grimpoteuthis sp. 4", "Grimpoteuthis sp. 5", "Grimpoteuthis tuftsi"))
    }

    test("decode ObservationsUpdate (camelCase") {
        val uuid = UUID.randomUUID()
        val json =
            s""" {
                |    "observationUuids": ["${uuid}"],
                |    "concept": "Grimpoteuthis",
                |    "observer": "svonthun",
                |    "group": "ROV",
                |    "activity": "stationary"
                |}""".stripMargin

//        println(json)
        val e = json.reify[ObservationsUpdate]
//        println(e)
        val opt = e.toOption

        assert(opt.isDefined)
        val ou = opt.get
        assertEquals(ou.observationUuids, List(uuid))
        assertEquals(ou.concept, Some("Grimpoteuthis"))
        assertEquals(ou.observer, Some("svonthun"))
        assertEquals(ou.group, Some("ROV"))
    }

    test("decode ObservationsUpdate (snake_case)") {
        val uuid = UUID.randomUUID()
        val json =
            s""" {
               |    "observation_uuids": ["${uuid}"],
               |    "concept": "Grimpoteuthis",
               |    "observer": "svonthun",
               |    "group": "ROV",
               |    "activity": "stationary"
               |}""".stripMargin

        //        println(json)
        val e = json.reify[ObservationsUpdate]
        //        println(e)
        val opt = e.toOption

        assert(opt.isDefined)
        val ou = opt.get
        assertEquals(ou.observationUuids, List(uuid))
        assertEquals(ou.concept, Some("Grimpoteuthis"))
        assertEquals(ou.observer, Some("svonthun"))
        assertEquals(ou.group, Some("ROV"))
    }

    test("decode constraints") {
        val url = getClass.getResource("/json/constraints.json")
        assert(url != null)
        val t   = Using(Source.fromURL(url)) { source =>
            val json = source.getLines().mkString
            assert(json != null)
            val opt = json.reify[Constraints].toOption
            assert(opt.isDefined)
            val constraints = opt.get.constraints
            assertEquals(constraints.size, 6)
        }
    }

}

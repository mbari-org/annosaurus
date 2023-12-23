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

package org.mbari.annosaurus.domain

import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import scala.jdk.CollectionConverters.*

class AnnotationSuite extends munit.FunSuite {

    val cc1 = DomainObjects.annotation

    test("camelCase/snake_case round trip") {

        val sc1 = cc1.toSnakeCase
        val cc2 = sc1.toCamelCase
        val sc2 = cc2.toSnakeCase
        assertEquals(cc2, cc1)
        assertEquals(sc1, sc2)
        assertEquals(sc2.activity, cc1.activity)
        assert(sc2.ancillary_data.isDefined)
        assertEquals(sc2.ancillary_data.get, sc1.ancillary_data.get)
        assertEquals(sc2.associations, sc1.associations)
        assertEquals(cc2.associations, cc1.associations)
        assertEquals(sc2.concept, cc1.concept)
        assertEquals(sc2.duration_millis, cc1.durationMillis)
        assertEquals(sc2.elapsed_time_millis, cc1.elapsedTimeMillis)
        assertEquals(sc2.group, cc1.group)
        assertEquals(sc2.imaged_moment_uuid, cc1.imagedMomentUuid)
        assert(sc2.image_references.nonEmpty)
        assertEquals(sc2.image_references, sc1.image_references)
        assertEquals(cc2.imageReferences, cc1.imageReferences)
        assertEquals(sc2.observation_timestamp, cc1.observationTimestamp)
        assertEquals(sc2.observation_uuid, cc1.observationUuid)
        assertEquals(sc2.observer, cc1.observer)
        assertEquals(sc2.recorded_timestamp, cc1.recordedTimestamp)
        assertEquals(sc2.timecode, cc1.timecode)
        assertEquals(sc2.video_reference_uuid, cc1.videoReferenceUuid)
        // println(cc2.stringify)
    }

    test("camelCase/Entity round trip") {
        val e1 = Annotation.toEntities(Seq(cc1), true).head
        val cc2 = Annotation.from(e1.getObservations().asScala.head, true)
        val e2 = Annotation.toEntities(Seq(cc2), true).head
        assertEquals(cc2, cc1)
        assertEquals(e2.getElapsedTime(), e1.getElapsedTime())
        assertEquals(e2.getTimecode().toString(), e1.getTimecode().toString())
        assertEquals(e2.getRecordedTimestamp(), e1.getRecordedTimestamp())
        assertEquals(e2.getVideoReferenceUuid(), e1.getVideoReferenceUuid())
        assertEquals(e2.getUuid(), e1.getUuid())
        assertEquals(e2.getObservations().size, e1.getObservations().size)

    }

}

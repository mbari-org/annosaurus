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

import org.mbari.annosaurus.AssertUtils
import org.mbari.annosaurus.controllers.TestUtils
import org.mbari.annosaurus.repository.jpa.entity.ImageReferenceEntity
import scala.jdk.CollectionConverters.*

import java.util

class AnnotationSuite extends munit.FunSuite {

    test("round trip / remove images") {
        val entity    = TestUtils.build(1, 3, 3, 3, true).head
        TestUtils.addRandomUuids(entity)
        val annotations = Annotation.fromImagedMoment(entity, true)
        assertEquals(annotations.size, entity.getObservations.size())
        val xs          = Annotation.toEntities(annotations, true)
        val obtained    = xs.head
        assertEquals(xs.size, 1)
        assertEquals(obtained.getObservations.size(), entity.getObservations.size())
        
        AssertUtils.assertSameImagedMoment(obtained, entity, true)
        

    }

    test("from") {
        val im  = TestUtils.build(1, 1, 1, 1, true).head
        TestUtils.addRandomUuids(im)
        val obs = im.getObservations.iterator().next()
        val a   = Annotation.from(obs, true)
        assertEquals(a.activity.orNull, obs.getActivity)
        assert(a.ancillaryData.isDefined)
        assertEquals(a.associations.size, 1)
        assertEquals(a.concept.orNull, obs.getConcept)
        assertEquals(a.duration.orNull, obs.getDuration)
        assertEquals(a.group.orNull, obs.getGroup)
        assertEquals(a.imagedMomentUuid.orNull, im.getUuid)
        assertEquals(a.imageReferences.size, 1)
        assertEquals(a.observationTimestamp.orNull, obs.getObservationTimestamp)
        assertEquals(a.observationUuid.orNull, obs.getUuid)
        assertEquals(a.observer.orNull, obs.getObserver)
        assertEquals(a.recordedTimestamp.orNull, im.getRecordedTimestamp)
        assertEquals(a.timecode, Option(im.getTimecode).map(_.toString))
        assertEquals(a.videoReferenceUuid.orNull, im.getVideoReferenceUuid)
    }

    test("round trip with extended graph") {
        val imagedMomentEntity  = TestUtils.build(1, 3, 3, 3, true).head
        TestUtils.addRandomUuids(imagedMomentEntity)

        // Sanity check
        assertEquals(imagedMomentEntity.getObservations.size(), 3)
        assertEquals(imagedMomentEntity.getImageReferences.size(), 3)
        for (obs <- imagedMomentEntity.getObservations.asScala) {
            assertEquals(obs.getAssociations.size(), 3)
        }


        // convert to annotations. Extend is true to add the ancillarydata. 
        val annotations = Annotation.fromImagedMoment(imagedMomentEntity, true)

        for 
            a <- annotations
        do 
            assertEquals(a.imageReferences.size, 3)
            assertEquals(a.associations.size, 3)

        // convert back to entities
        val entities = Annotation.toEntities(annotations)
        assertEquals(entities.size, 1)
        val obtained = entities.head
        AssertUtils.assertSameImagedMoment(imagedMomentEntity, obtained, true)

    }

}

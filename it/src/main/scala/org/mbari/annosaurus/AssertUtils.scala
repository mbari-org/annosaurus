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

package org.mbari.annosaurus

import org.junit.Assert._
import org.mbari.annosaurus.model.{MutableAssociation, CachedAncillaryDatum, MutableImageReference, MutableObservation}
import org.mbari.annosaurus.repository.jpa.entity.ImagedMomentEntity

object AssertUtils {

    def assertSameImagedMoment(a: ImagedMomentEntity, b: ImagedMomentEntity, cascade: Boolean = true): Unit = {
        if (a == null && b == null) {
            // do nothing
        }
        else if (a != null && b != null) {
            assertEquals(a.timecode, b.timecode)
            assertEquals(a.elapsedTime, b.elapsedTime)
            assertEquals(a.recordedDate, b.recordedDate)
            assertEquals(a.videoReferenceUUID, b.videoReferenceUUID)
            assertEquals(a.uuid, b.uuid)
            if (cascade) {
                assertEquals(a.observations.size, b.observations.size)
                val ax = a.observations.toSeq.sortBy(_.uuid)
                val bx = b.observations.toSeq.sortBy(_.uuid)
                ax.zip(bx).foreach(p => assertSameObservation(p._1, p._2, cascade))

                assertEquals(a.imageReferences.size, b.imageReferences.size)
                val ay = a.imageReferences.toSeq.sortBy(_.uuid)
                val by = b.imageReferences.toSeq.sortBy(_.uuid)
                ay.zip(by).foreach(p => assertSameImageReference(p._1, p._2))

                assertEquals(a.ancillaryDatum, b.ancillaryDatum)
            }
        }
        else {
            fail("One of the ImagedMoments is null")
        }
    }

    def assertSameObservation(a: MutableObservation, b: MutableObservation, cascade: Boolean = true): Unit = {
        if (a == null && b == null) {
            // do nothing
        }
        else if (a != null && b != null) {
            assertEquals(a.concept, b.concept)
            assertEquals(a.group, b.group)
            assertEquals(a.activity, b.activity)
            assertEquals(a.duration, b.duration)
            assertEquals(a.observer, b.observer)
            assertEquals(a.uuid, b.uuid)
            if (cascade) {
                assertEquals(a.associations.size, b.associations.size)
                val ax = a.associations.toSeq.sortBy(_.uuid)
                val bx = b.associations.toSeq.sortBy(_.uuid)
                ax.zip(bx).foreach(p => assertSameAssociation(p._1, p._2))
            }
        }
        else {
            fail("One of the observations is null")
        }

    }

    def assertSameImageReference(a: MutableImageReference, b: MutableImageReference): Unit = {
        if (a == null && b == null) {
            // do nothing
        }
        else if (a != null && b != null) {
            assertEquals(a.uuid, b.uuid)
            assertEquals(a.url, b.url)
            assertEquals(a.width, b.width)
            assertEquals(a.height, b.height)
            assertEquals(a.description, b.description)
            assertEquals(a.format, b.format)
        }
        else {
            fail("One of the imagereferences is null")
        }
    }

    def assertSameAssociation(a: MutableAssociation, b: MutableAssociation): Unit = {
        if (a == null && b == null) {
            // do nothing
        }
        else if (a != null && b != null) {
            assertEquals(a.uuid, b.uuid)
            assertEquals(a.linkName, b.linkName)
            assertEquals(a.toConcept, b.toConcept)
            assertEquals(a.linkValue, b.linkValue)
            assertEquals(a.mimeType, b.mimeType)
        }
        else {
            fail("One of the associations is null")
        }
    }

    def assertSameAncillaryDatum(a: CachedAncillaryDatum, b: CachedAncillaryDatum): Unit = {
        if (a == null && b == null) {
            // do nothing
        }
        else if (a != null && b != null) {
            assertEquals(a.uuid, b.uuid)
            assertEquals(a.x, b.x)
            assertEquals(a.y, b.y)
            assertEquals(a.crs, b.crs)
            assertEquals(a.latitude, b.latitude)
            assertEquals(a.longitude, b.longitude)
            assertEquals(a.altitude, b.altitude)
            assertEquals(a.depthMeters, b.depthMeters)
            assertEquals(a.pressureDbar, b.pressureDbar)
            assertEquals(a.posePositionUnits, b.posePositionUnits)
            assertEquals(a.lightTransmission, b.lightTransmission)
            assertEquals(a.oxygenMlL, b.oxygenMlL)
            assertEquals(a.salinity, b.salinity)
            assertEquals(a.temperatureCelsius, b.temperatureCelsius)
        }
        else {
            fail("One of the ancillarydata is null")
        }
    }

}

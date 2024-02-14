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

import org.mbari.annosaurus.repository.jpa.entity.ImagedMomentEntity
import org.mbari.annosaurus.repository.jpa.entity.ObservationEntity
import org.mbari.annosaurus.repository.jpa.entity.ImageReferenceEntity
import org.mbari.annosaurus.repository.jpa.entity.AssociationEntity
import org.mbari.annosaurus.repository.jpa.entity.CachedAncillaryDatumEntity
import scala.jdk.CollectionConverters.*
import org.mbari.annosaurus.domain.CachedVideoReferenceInfo
import org.mbari.annosaurus.repository.jpa.entity.CachedVideoReferenceInfoEntity
import org.mbari.annosaurus.repository.jpa.entity.IndexEntity

object AssertUtils {

    def assertSameImagedMoment(
        a: ImagedMomentEntity,
        b: ImagedMomentEntity,
        cascade: Boolean = true
    ): Unit = {
        if (a == null && b == null) {
            // do nothing
        }
        else if (a != null && b != null) {
            // assertEquals(a.getTimecode(), b.getTimecode())
            if (a.getTimecode() != null && b.getTimecode() != null) {
                assertEquals(a.getTimecode().toString(), b.getTimecode().toString())
            }
            else if (a.getTimecode() == null && b.getTimecode() == null) {
                // do nothing
            }
            else {
                fail("One of the timecodes is null")
            }
            assertEquals(a.getElapsedTime(), b.getElapsedTime())
            assertEquals(a.getRecordedTimestamp(), b.getRecordedTimestamp())
            assertEquals(a.getVideoReferenceUuid(), b.getVideoReferenceUuid())
            assertEquals(a.getUuid(), b.getUuid())
            if (cascade) {
                assertEquals(a.getObservations.size, b.getObservations.size)
                val ax = Option(a.getObservations)
                    .map(_.asScala.toSeq.sortBy(_.getUuid()))
                    .getOrElse(Seq.empty)
                val bx = Option(b.getObservations).map(_.asScala.toSeq.sortBy(_.getUuid())).getOrElse(Seq.empty)
                ax.zip(bx).foreach(p => assertSameObservation(p._1, p._2, cascade))

                assertEquals(a.getImageReferences.size, b.getImageReferences.size)
                val ay = Option(a.getImageReferences).map(_.asScala.toSeq.sortBy(_.getUuid())).getOrElse(Seq.empty)
                val by = Option(b.getImageReferences).map(_.asScala.toSeq.sortBy(_.getUuid())).getOrElse(Seq.empty)
                ay.zip(by).foreach(p => assertSameImageReference(p._1, p._2))

                assertSameAncillaryDatum(a.getAncillaryDatum(), b.getAncillaryDatum())
            }
        }
        else {
            fail("One of the ImagedMoments is null")
        }
    }
    

    def assertSameObservation(
        a: ObservationEntity,
        b: ObservationEntity,
        cascade: Boolean = true
    ): Unit = {
        if (a == null && b == null) {
            // do nothing
        }
        else if (a != null && b != null) {
            assertEquals(a.getConcept(), b.getConcept())
            assertEquals(a.getGroup(), b.getGroup())
            assertEquals(a.getActivity(), b.getActivity())
            assertEquals(a.getDuration(), b.getDuration())
            assertEquals(a.getObserver(), b.getObserver())
            assertEquals(a.getUuid(), b.getUuid())
            if (cascade) {
                assertEquals(a.getAssociations().size, b.getAssociations().size)
                val ax = a.getAssociations().asScala.toSeq.sortBy(_.getUuid)
                val bx = b.getAssociations().asScala.toSeq.sortBy(_.getUuid)
                ax.zip(bx).foreach(p => assertSameAssociation(p._1, p._2))
            }
        }
        else {
            fail("One of the observations is null")
        }

    }

    def assertSameImageReference(a: ImageReferenceEntity, b: ImageReferenceEntity): Unit = {
        if (a == null && b == null) {
            // do nothing
        }
        else if (a != null && b != null) {
            assertEquals(a.getUuid, b.getUuid())
            assertEquals(a.getUrl(), b.getUrl())
            assertEquals(a.getWidth(), b.getWidth())
            assertEquals(a.getHeight(), b.getHeight())
            assertEquals(a.getDescription(), b.getDescription())
            assertEquals(a.getFormat(), b.getFormat())
        }
        else {
            fail("One of the imagereferences is null")
        }
    }

    def assertSameAssociation(a: AssociationEntity, b: AssociationEntity): Unit = {
        if (a == null && b == null) {
            // do nothing
        }
        else if (a != null && b != null) {
            assertEquals(a.getUuid(), b.getUuid())
            assertEquals(a.getLinkName(), b.getLinkName())
            assertEquals(a.getToConcept(), b.getToConcept())
            assertEquals(a.getLinkValue(), b.getLinkValue())
            assertEquals(a.getMimeType(), b.getMimeType())
        }
        else {
            fail("One of the associations is null")
        }
    }

    def assertSameAncillaryDatum(
        a: CachedAncillaryDatumEntity,
        b: CachedAncillaryDatumEntity
    ): Unit = {
        if (a == null && b == null) {
            // do nothing
        }
        else if (a != null && b != null) {
            assertEquals(a.getUuid(), b.getUuid())
            assertEquals(a.getX(), b.getX())
            assertEquals(a.getY(), b.getY())
            assertEquals(a.getZ(), b.getZ())
            assertEquals(a.getCrs(), b.getCrs())
            assertEquals(a.getLatitude(), b.getLatitude())
            assertEquals(a.getLongitude(), b.getLongitude())
            assertEquals(a.getAltitude(), b.getAltitude())
            assertEquals(a.getDepthMeters(), b.getDepthMeters())
            assertEquals(a.getPressureDbar(), b.getPressureDbar())
            assertEquals(a.getPosePositionUnits(), b.getPosePositionUnits())
            assertEquals(a.getLightTransmission(), b.getLightTransmission())
            assertEquals(a.getOxygenMlL(), b.getOxygenMlL())
            assertEquals(a.getSalinity(), b.getSalinity())
            assertEquals(a.getTemperatureCelsius(), b.getTemperatureCelsius())
        }
        else {
            fail("One of the ancillarydata is null")
        }
    }

    def assertSameVideoReferenceInfo(
        a: CachedVideoReferenceInfoEntity,
        b: CachedVideoReferenceInfoEntity
    ): Unit = {
        if (a == null && b == null) {
            // do nothing
        }
        else if (a != null && b != null) {
            assertEquals(a.getUuid(), b.getUuid())
            assertEquals(a.getVideoReferenceUuid(), b.getVideoReferenceUuid())
            assertEquals(a.getMissionContact(), b.getMissionContact())
            assertEquals(a.getMissionId(), b.getMissionId())
            assertEquals(a.getPlatformName(), b.getPlatformName())
        }
    }

    def assertSameIndex(
        a: IndexEntity,
        b: IndexEntity
    ): Unit = {
        if (a == null && b == null) {
            // do nothing
        }
        else if (a != null && b != null) {
            assertEquals(a.getUuid(), b.getUuid())
            assertEquals(a.getVideoReferenceUuid(), b.getVideoReferenceUuid())
            assertEquals(a.getElapsedTime(), b.getElapsedTime())
            assertEquals(a.getRecordedTimestamp(), b.getRecordedTimestamp())
            if (a.getTimecode() != null && b.getTimecode() != null) {
                assertEquals(a.getTimecode().toString(), b.getTimecode().toString())
            }
            else if (a.getTimecode() == null && b.getTimecode() == null) {
                // do nothing
            }
            else {
                fail("One of the timecodes is null")
            }
        }
    }

}

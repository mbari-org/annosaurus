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

package org.mbari.annosaurus.repository


import java.util.UUID
import org.mbari.annosaurus.repository.jpa.entity.IPersistentObject

/** @author
  *   Brian Schlining
  * @since 2016-06-17T16:08:00
  */
trait CachedAncillaryDatumDAO[T <: IPersistentObject] extends DAO[T] {

    def newPersistentObject(
        latitude: Double,
        longitude: Double,
        depthMeters: Double,
        altitudeMeters: Option[Double] = None,
        crs: Option[String] = None,
        salinity: Option[Double] = None,
        temperatureCelsius: Option[Double] = None,
        oxygenMlL: Option[Double] = None,
        pressureDbar: Option[Double] = None,
        lightTransmission: Option[Double] = None,
        x: Option[Double] = None,
        y: Option[Double] = None,
        z: Option[Double] = None,
        posePositionUnits: Option[String] = None,
        phi: Option[Double] = None,
        theta: Option[Double] = None,
        psi: Option[Double] = None
    ): T

    def findByObservationUUID(observationUuid: UUID): Option[T]

    def findByImagedMomentUUID(imagedMomentUuid: UUID): Option[T]

    def newPersistentObject(bean: T): T

    /** Delete all ancillary data associated with annotations for a given video reference
      * @param videoReferenceUuid
      *   the VideoReference UUID
      * @return
      *   The number of rows deleted
      */
    def deleteByVideoReferenceUuid(videoReferenceUuid: UUID): Int

}

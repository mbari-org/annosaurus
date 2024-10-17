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

package org.mbari.annosaurus.repository.jpa

import java.util.UUID
import jakarta.persistence.EntityManager
import org.mbari.annosaurus.etc.jdk.Numbers.*
import org.mbari.annosaurus.repository.CachedAncillaryDatumDAO
import org.mbari.annosaurus.repository.jpa.entity.{AncillaryDatumDTO, CachedAncillaryDatumEntity}

/**
 * @author
 *   Brian Schlining
 * @since 2016-06-17T17:12:00
 */
class CachedAncillaryDatumDAOImpl(entityManager: EntityManager)
    extends BaseDAO[CachedAncillaryDatumEntity](entityManager)
    with CachedAncillaryDatumDAO[CachedAncillaryDatumEntity]:

    override def newPersistentObject(): CachedAncillaryDatumEntity = new CachedAncillaryDatumEntity

    def newPersistentObject(
        latitude: Double,
        longitude: Double,
        depthMeters: Float,
        altitude: Option[Float] = None,
        crs: Option[String] = None,
        salinity: Option[Float] = None,
        temperatureCelsius: Option[Float] = None,
        oxygenMlL: Option[Float] = None,
        pressureDbar: Option[Float] = None,
        lightTransmission: Option[Float] = None,
        x: Option[Double] = None,
        y: Option[Double] = None,
        z: Option[Double] = None,
        posePositionUnits: Option[String] = None,
        phi: Option[Double] = None,
        theta: Option[Double] = None,
        psi: Option[Double] = None
    ): CachedAncillaryDatumEntity =

        val cad = new CachedAncillaryDatumEntity()

        cad.setLatitude(latitude)
        cad.setLongitude(longitude)
        cad.setDepthMeters(depthMeters)
        altitude.foreach(cad.setAltitude(_))
        crs.foreach(cad.setCrs)
        salinity.foreach(cad.setSalinity(_))
        temperatureCelsius.foreach(cad.setTemperatureCelsius(_))
        oxygenMlL.foreach(cad.setOxygenMlL(_))
        pressureDbar.foreach(cad.setPressureDbar(_))
        lightTransmission.foreach(cad.setLightTransmission(_))
        x.foreach(cad.setX(_))
        y.foreach(cad.setY(_))
        z.foreach(cad.setZ(_))
        posePositionUnits.foreach(cad.setPosePositionUnits)
        phi.foreach(cad.setPhi(_))
        theta.foreach(cad.setTheta(_))
        psi.foreach(cad.setPsi(_))

        cad

    override def newPersistentObject(
        datum: CachedAncillaryDatumEntity
    ): CachedAncillaryDatumEntity =
        CachedAncillaryDatumEntity(datum)

    override def findAll(
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): Iterable[CachedAncillaryDatumEntity] =
        findByNamedQuery("AncillaryDatum.findAll", limit = limit, offset = offset)

    override def findByObservationUUID(observationUuid: UUID): Option[CachedAncillaryDatumEntity] =
        findByNamedQuery(
            "AncillaryDatum.findByObservationUUID",
            Map("uuid" -> observationUuid)
        ).headOption

    override def findDTOByObservationUuid(observationUuid: UUID): Option[AncillaryDatumDTO] =
        findByTypedNamedQuery(
            "AncillaryDatum.findDTOByObservationUUID",
            Map("uuid" -> observationUuid)
        ).headOption

    override def findByImagedMomentUUID(
        imagedMomentUuid: UUID
    ): Option[CachedAncillaryDatumEntity] =
        findByNamedQuery(
            "AncillaryDatum.findByImagedMomentUUID",
            Map("uuid" -> imagedMomentUuid)
        ).headOption

    override def deleteByVideoReferenceUuid(videoReferenceUuid: UUID): Int =
        val query = entityManager.createNamedQuery("AncillaryDatum.deleteByVideoReferenceUuid")
        query.setParameter(1, videoReferenceUuid)
        query.executeUpdate()

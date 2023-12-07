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

import java.util.UUID
import jakarta.persistence.EntityManager

import org.mbari.vars.annotation.dao.CachedAncillaryDatumDAO
import org.mbari.vars.annotation.model.CachedAncillaryDatum

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-06-17T17:12:00
  */
class CachedAncillaryDatumDAOImpl(entityManager: EntityManager)
    extends BaseDAO[CachedAncillaryDatumEntity](entityManager)
    with CachedAncillaryDatumDAO[CachedAncillaryDatumEntity] {

  override def newPersistentObject(): CachedAncillaryDatumEntity = new CachedAncillaryDatumEntity

  def newPersistentObject(
      latitude: Double,
      longitude: Double,
      depthMeters: Double,
      altitude: Option[Double] = None,
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
  ): CachedAncillaryDatum = {

    val cad = new CachedAncillaryDatumEntity()
    cad.latitude = Some(latitude)
    cad.longitude = Some(longitude)
    cad.depthMeters = Some(depthMeters)
    cad.altitude = altitude
    crs.foreach(cad.crs = _)
    cad.salinity = salinity
    cad.temperatureCelsius = temperatureCelsius
    cad.oxygenMlL = oxygenMlL
    cad.pressureDbar = pressureDbar
    cad.x = x
    cad.y = y
    cad.z = z
    posePositionUnits.foreach(cad.posePositionUnits = _)
    cad.phi = phi
    cad.theta = theta
    cad.psi = psi
    cad
  }

  override def newPersistentObject(datum: CachedAncillaryDatum): CachedAncillaryDatum =
    CachedAncillaryDatumEntity(datum)

  override def findAll(
      limit: Option[Int] = None,
      offset: Option[Int] = None
  ): Iterable[CachedAncillaryDatumEntity] =
    findByNamedQuery("AncillaryDatum.findAll", limit = limit, offset = offset)

  override def findByObservationUUID(observationUuid: UUID): Option[CachedAncillaryDatum] =
    findByNamedQuery("AncillaryDatum.findByObservationUUID", Map("uuid" -> observationUuid)).headOption

  override def findByImagedMomentUUID(imagedMomentUuid: UUID): Option[CachedAncillaryDatum] =
    findByNamedQuery("AncillaryDatum.findByImagedMomentUUID", Map("uuid" -> imagedMomentUuid)).headOption

  override def deleteByVideoReferenceUuid(videoReferenceUuid: UUID): Int = {
    val query = entityManager.createNamedQuery("AncillaryDatum.deleteByVideoReferenceUuid")
    query.setParameter(1, videoReferenceUuid)
    query.executeUpdate()
  }
}

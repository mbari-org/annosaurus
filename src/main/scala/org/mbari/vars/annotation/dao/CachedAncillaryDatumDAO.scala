package org.mbari.vars.annotation.dao

import java.util.UUID

import org.mbari.vars.annotation.model.CachedAncillaryDatum

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T16:08:00
 */
trait CachedAncillaryDatumDAO[T <: CachedAncillaryDatum] extends DAO[T] {

  def newPersistentObject(
    latitude: Double,
    longitude: Double,
    depthMeters: Float,
    altitudeMeters: Option[Float] = None,
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
  ): CachedAncillaryDatum

  def findByObservationUUID(observationUuid: UUID): Option[CachedAncillaryDatum]

  def findByImagedMomentUUID(imagedMomentUuid: UUID): Option[CachedAncillaryDatum]

}

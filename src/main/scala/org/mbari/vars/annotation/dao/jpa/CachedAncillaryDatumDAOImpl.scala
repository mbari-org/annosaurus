package org.mbari.vars.annotation.dao.jpa

import java.util.UUID
import javax.persistence.EntityManager

import org.mbari.vars.annotation.dao.CachedAncillaryDatumDAO
import org.mbari.vars.annotation.model.CachedAncillaryDatum

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T17:12:00
 */
class CachedAncillaryDatumDAOImpl(entityManager: EntityManager)
    extends BaseDAO[CachedAncillaryDatumImpl](entityManager)
    with CachedAncillaryDatumDAO[CachedAncillaryDatumImpl] {

  override def newPersistentObject(): CachedAncillaryDatumImpl = new CachedAncillaryDatumImpl

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
  ): CachedAncillaryDatum = {

    val cad = new CachedAncillaryDatumImpl()
    cad.latitude = latitude
    cad.longitude = longitude
    cad.depthMeters = depthMeters
    altitude.foreach(cad.altitude = _)
    crs.foreach(cad.crs = _)
    salinity.foreach(cad.salinity = _)
    temperatureCelsius.foreach(cad.temperatureCelsius = _)
    oxygenMlL.foreach(cad.oxygenMlL = _)
    pressureDbar.foreach(cad.pressureDbar = _)
    x.foreach(cad.x = _)
    y.foreach(cad.y = _)
    z.foreach(cad.z = _)
    posePositionUnits.foreach(cad.posePositionUnits = _)
    phi.foreach(cad.phi = _)
    theta.foreach(cad.theta = _)
    psi.foreach(cad.psi = _)
    cad
  }

  override def findAll(): Iterable[CachedAncillaryDatumImpl] =
    findByNamedQuery("AncillaryDatum.findAll")

  override def findAll(limit: Int, offset: Int): Iterable[CachedAncillaryDatumImpl] =
    findByNamedQuery("AncillaryDatum.findAll", limit = Some(limit), offset = Some(offset))

  override def findByObservationUUID(observationUuid: UUID): Option[CachedAncillaryDatum] =
    findByNamedQuery(
      "AncillaryDatum.findByObservationUUID",
      Map("uuid" -> observationUuid)
    )
      .headOption

  override def findByImagedMomentUUID(imagedMomentUuid: UUID): Option[CachedAncillaryDatum] =
    findByNamedQuery(
      "AncillaryDatum.findByImagedMomentUUID",
      Map("uuid" -> imagedMomentUuid)
    )
      .headOption
}

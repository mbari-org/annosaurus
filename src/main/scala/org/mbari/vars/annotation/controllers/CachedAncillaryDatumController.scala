package org.mbari.vars.annotation.controllers

import java.util.UUID

import org.mbari.vars.annotation.dao.{ CachedAncillaryDatumDAO, NotFoundInDatastoreException }
import org.mbari.vars.annotation.model.CachedAncillaryDatum

import scala.concurrent.{ ExecutionContext, Future }

/**
 * @author Brian Schlining
 * @since 2017-05-01T10:53:00
 */
class CachedAncillaryDatumController(val daoFactory: BasicDAOFactory)
    extends BaseController[CachedAncillaryDatum, CachedAncillaryDatumDAO[CachedAncillaryDatum]] {

  protected type ADDAO = CachedAncillaryDatumDAO[CachedAncillaryDatum]

  override def newDAO(): CachedAncillaryDatumDAO[CachedAncillaryDatum] = daoFactory.newCachedAncillaryDatumDAO()

  def create(
    imagedMomentUuid: UUID,
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
  )(implicit ec: ExecutionContext): Future[CachedAncillaryDatum] = {

    def fn(dao: ADDAO): CachedAncillaryDatum = {
      val imDao = daoFactory.newImagedMomentDAO(dao)
      imDao.findByUUID(imagedMomentUuid) match {
        case None =>
          throw new NotFoundInDatastoreException(s"ImagedMoment with UUID of $imagedMomentUuid was no found")
        case Some(imagedMoment) =>
          if (imagedMoment.ancillaryDatum != null) {
            throw new RuntimeException(s"ImagedMoment with UUID of $imagedMomentUuid already has ancillary data")
          } else {
            val cad = dao.newPersistentObject(latitude, longitude, depthMeters, altitude, crs,
              salinity, temperatureCelsius, oxygenMlL, pressureDbar, lightTransmission,
              x, y, z, posePositionUnits, phi, theta, psi)
            imagedMoment.ancillaryDatum = cad
            cad
          }
      }
    }
    exec(fn)
  }

  def update(
    uuid: UUID,
    latitude: Option[Double] = None,
    longitude: Option[Double] = None,
    depthMeters: Option[Float] = None,
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
  )(implicit ec: ExecutionContext): Future[Option[CachedAncillaryDatum]] = {

    def fn(dao: ADDAO): Option[CachedAncillaryDatum] = {
      dao.findByUUID(uuid).map(cad => {
        latitude.foreach(cad.latitude = _)
        longitude.foreach(cad.longitude = _)
        depthMeters.foreach(cad.depthMeters = _)
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
      })
    }
    exec(fn)
  }

  def findByObservationUUID(uuid: UUID)(implicit ec: ExecutionContext): Future[Option[CachedAncillaryDatum]] = {
    def fn(dao: ADDAO): Option[CachedAncillaryDatum] = dao.findByObservationUUID(uuid)
    exec(fn)
  }

  def findByImagedMomentUUID(uuid: UUID)(implicit ec: ExecutionContext): Future[Option[CachedAncillaryDatum]] = {
    def fn(dao: ADDAO): Option[CachedAncillaryDatum] = dao.findByImagedMomentUUID(uuid)
    exec(fn)
  }
}

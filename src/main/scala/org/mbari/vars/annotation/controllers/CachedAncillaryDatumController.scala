package org.mbari.vars.annotation.controllers

import java.util.UUID

import org.mbari.vars.annotation.dao.{ CachedAncillaryDatumDAO, NotFoundInDatastoreException }
import org.mbari.vars.annotation.model.CachedAncillaryDatum
import org.mbari.vars.annotation.model.simple.CachedAncillaryDatumBean
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContext, Future }

/**
 * @author Brian Schlining
 * @since 2017-05-01T10:53:00
 */
class CachedAncillaryDatumController(val daoFactory: BasicDAOFactory)
    extends BaseController[CachedAncillaryDatum, CachedAncillaryDatumDAO[CachedAncillaryDatum]] {

  protected type ADDAO = CachedAncillaryDatumDAO[CachedAncillaryDatum]
  private[this] val log = LoggerFactory.getLogger(getClass)

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

  def create(imagedMomentUuid: UUID, datum: CachedAncillaryDatum)(implicit ec: ExecutionContext): Future[CachedAncillaryDatum] = {
    def fn(dao: ADDAO): CachedAncillaryDatum = {
      val imDao = daoFactory.newImagedMomentDAO(dao)
      imDao.findByUUID(imagedMomentUuid) match {
        case None =>
          throw new NotFoundInDatastoreException(s"ImagedMoment with UUID of $imagedMomentUuid was no found")
        case Some(imagedMoment) =>
          if (imagedMoment.ancillaryDatum != null) {
            throw new RuntimeException(s"ImagedMoment with UUID of $imagedMomentUuid already has ancillary data")
          } else {
            imagedMoment.ancillaryDatum = datum
            datum
          }
      }
    }
    exec(fn)
  }

  def create(datum: CachedAncillaryDatumBean)(implicit ec: ExecutionContext): Future[CachedAncillaryDatum] =
    create(datum.imagedMomentUuid, datum)

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
        cad.latitude = latitude
        cad.longitude = longitude
        cad.depthMeters = depthMeters
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

  def bulkCreateOrUpdate(data: Iterable[CachedAncillaryDatumBean])(implicit ec: ExecutionContext): Future[Seq[CachedAncillaryDatum]] = {
    def fn(dao: ADDAO): Seq[CachedAncillaryDatum] = {
      val imDao = daoFactory.newImagedMomentDAO(dao)
      val cads = for {
        datum <- data
        if datum.imagedMomentUuid != null
      } yield {
        val c = dao.asPersistentObject(datum)
        imDao.findByUUID(datum.imagedMomentUuid)
          .flatMap(im => if (im.ancillaryDatum != null) {
            log.info(s"ImagedMoment with UUID of ${datum.imagedMomentUuid} already has ancillary data")
            None
          } else {
            if (im.ancillaryDatum != null) {
              datum.uuid = im.ancillaryDatum.uuid
              im.ancillaryDatum = datum
              Some(dao.update(datum))

            } else {
              im.ancillaryDatum = datum
              Some(datum)
            }
          })
      }
      cads.flatten.toSeq
    }
    exec(fn)
  }
}

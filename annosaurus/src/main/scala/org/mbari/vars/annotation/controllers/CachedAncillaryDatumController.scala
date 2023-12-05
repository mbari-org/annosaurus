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

package org.mbari.vars.annotation.controllers

import java.time.Duration
import java.util.UUID

import org.mbari.vars.annotation.dao.jpa.BaseDAO
import org.mbari.vars.annotation.dao.{CachedAncillaryDatumDAO, NotFoundInDatastoreException}
import org.mbari.vars.annotation.math.FastCollator
import org.mbari.vars.annotation.model.{CachedAncillaryDatum, ImagedMoment}
import org.mbari.vars.annotation.model.simple.CachedAncillaryDatumBean
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Brian Schlining
  * @since 2017-05-01T10:53:00
  */
class CachedAncillaryDatumController(val daoFactory: BasicDAOFactory)
    extends BaseController[CachedAncillaryDatum, CachedAncillaryDatumDAO[CachedAncillaryDatum]] {

  protected type ADDAO = CachedAncillaryDatumDAO[CachedAncillaryDatum]
  LoggerFactory.getLogger(getClass)

  override def newDAO(): CachedAncillaryDatumDAO[CachedAncillaryDatum] =
    daoFactory.newCachedAncillaryDatumDAO()

  def create(
      imagedMomentUuid: UUID,
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
  )(implicit ec: ExecutionContext): Future[CachedAncillaryDatum] = {

    def fn(dao: ADDAO): CachedAncillaryDatum = {
      val imDao = daoFactory.newImagedMomentDAO(dao)
      imDao.findByUUID(imagedMomentUuid) match {
        case None =>
          throw new NotFoundInDatastoreException(
            s"ImagedMoment with UUID of $imagedMomentUuid was no found"
          )
        case Some(imagedMoment) =>
          if (imagedMoment.ancillaryDatum != null) {
            throw new RuntimeException(
              s"ImagedMoment with UUID of $imagedMomentUuid already has ancillary data"
            )
          }
          else {
            val cad = dao.newPersistentObject(
              latitude,
              longitude,
              depthMeters,
              altitude,
              crs,
              salinity,
              temperatureCelsius,
              oxygenMlL,
              pressureDbar,
              lightTransmission,
              x,
              y,
              z,
              posePositionUnits,
              phi,
              theta,
              psi
            )
            imagedMoment.ancillaryDatum = cad
            cad
          }
      }
    }

    exec(fn)
  }

  def create(imagedMomentUuid: UUID, datum: CachedAncillaryDatum)(
      implicit ec: ExecutionContext
  ): Future[CachedAncillaryDatum] = {
    def fn(dao: ADDAO): CachedAncillaryDatum = {
      val imDao = daoFactory.newImagedMomentDAO(dao)
      imDao.findByUUID(imagedMomentUuid) match {
        case None =>
          throw new NotFoundInDatastoreException(
            s"ImagedMoment with UUID of $imagedMomentUuid was no found"
          )
        case Some(imagedMoment) =>
          if (imagedMoment.ancillaryDatum != null) {
            throw new RuntimeException(
              s"ImagedMoment with UUID of $imagedMomentUuid already has ancillary data"
            )
          }
          else {
            imagedMoment.ancillaryDatum = datum
            datum
          }
      }
    }

    exec(fn)
  }

  def create(
      datum: CachedAncillaryDatumBean
  )(implicit ec: ExecutionContext): Future[CachedAncillaryDatum] =
    create(datum.imagedMomentUuid, datum)

  def update(
      uuid: UUID,
      latitude: Option[Double] = None,
      longitude: Option[Double] = None,
      depthMeters: Option[Double] = None,
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
  )(implicit ec: ExecutionContext): Future[Option[CachedAncillaryDatum]] = {

    def fn(dao: ADDAO): Option[CachedAncillaryDatum] = {
      dao
        .findByUUID(uuid)
        .map(cad => {
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

  def findByVideoReferenceUUID(
      uuid: UUID
  )(implicit ec: ExecutionContext): Future[Seq[CachedAncillaryDatumBean]] = {
    def fn(dao: ADDAO): Seq[CachedAncillaryDatumBean] = {
      val imDao   = daoFactory.newImagedMomentDAO(dao)
      val moments = imDao.findByVideoReferenceUUID(uuid)
      moments
        .filter(_.ancillaryDatum != null)
        .map(im => CachedAncillaryDatumBean(im.ancillaryDatum))
        .toSeq
    }

    exec(fn)
  }

  def findByObservationUUID(
      uuid: UUID
  )(implicit ec: ExecutionContext): Future[Option[CachedAncillaryDatum]] = {
    def fn(dao: ADDAO): Option[CachedAncillaryDatum] = dao.findByObservationUUID(uuid)

    exec(fn)
  }

  def findByImagedMomentUUID(
      uuid: UUID
  )(implicit ec: ExecutionContext): Future[Option[CachedAncillaryDatum]] = {
    def fn(dao: ADDAO): Option[CachedAncillaryDatum] = dao.findByImagedMomentUUID(uuid)

    exec(fn)
  }

  def bulkCreateOrUpdate(
      data: Seq[CachedAncillaryDatumBean]
  )(implicit ec: ExecutionContext): Future[Seq[CachedAncillaryDatum]] = {
    def fn(dao: ADDAO): Seq[CachedAncillaryDatum] = {
      val fastDao = new FastAncillaryDataController(dao.asInstanceOf[BaseDAO[_]].entityManager)
      fastDao.createOrUpdate(data)
      data

      //      val imDao = daoFactory.newImagedMomentDAO(dao)
      //      val cads = for {
      //        datum <- data
      //        imagedMomentUuid <- Option(datum.imagedMomentUuid)
      //      } yield {
      //        val maybeMoment = imDao.findByUUID(imagedMomentUuid)
      //        val cad = dao.asPersistentObject(datum)
      //        val existingCad = dao.findByImagedMomentUUID(imagedMomentUuid)
      //        maybeMoment.flatMap(im => Option(createOrUpdate(cad, im)))
      //      }
      //      cads.flatten
    }

    exec(fn)
  }

  /**
    * This method should be called within a transaction!
    *
    * @param d  This MUST be a persistable object! (Not a CahcedAncillaryDatumBean)
    * @param im The moment whose ancillary data is being updated
    * @return The CachedAncillaryDatum.
    */
  private def createOrUpdate(d: CachedAncillaryDatum, im: ImagedMoment): CachedAncillaryDatum = {
    require(d != null, "A null CachedAncillaryDatum argument is not allowed")
    require(im != null, "A null ImagedMoment argument is not allowed")
    require(
      im.uuid != null,
      "The ImagedMoment should already be present in the database. (Null UUID was found"
    )
    require(!d.isInstanceOf[CachedAncillaryDatumBean], "Can not persist a CachedAncillaryDatumBean")

    if (im.ancillaryDatum != null) {
      updateValues(im.ancillaryDatum, d)
      im.ancillaryDatum
    }
    else {
      im.ancillaryDatum = d
      im.ancillaryDatum
    }
  }

  def merge(
      data: Iterable[CachedAncillaryDatumBean],
      videoReferenceUuid: UUID,
      tolerance: Duration = Duration.ofMillis(7500)
  )(implicit ec: ExecutionContext): Future[Seq[CachedAncillaryDatum]] = {

    def fn(dao: ADDAO): Seq[CachedAncillaryDatum] = {
      val imDao = daoFactory.newImagedMomentDAO(dao)
      val imagedMoments = imDao
        .findByVideoReferenceUUID(videoReferenceUuid)
        .filter(ir => ir.recordedDate != null)

      val usefulData = data.filter(_.recordedTimestamp.isDefined)

      def imagedMomentToMillis(im: ImagedMoment) = im.recordedDate.toEpochMilli.toDouble

      def datumToMillis(cd: CachedAncillaryDatumBean) =
        cd.recordedTimestamp.map(_.toEpochMilli).getOrElse(-1L).toDouble

      val mergedData = FastCollator(
        imagedMoments,
        imagedMomentToMillis,
        usefulData,
        datumToMillis,
        tolerance.toMillis.toDouble
      )

      for {
        (im, opt) <- mergedData
        cad       <- opt
      } yield {
        val d = dao.newPersistentObject(cad)
        createOrUpdate(d, im)
      }
    }

    exec(fn)
  }

  def deleteByVideoReferenceUuid(
      videoReferenceUuid: UUID
  )(implicit ec: ExecutionContext): Future[Int] = {
    def fn(dao: ADDAO): Int = dao.deleteByVideoReferenceUuid(videoReferenceUuid)

    exec(fn)
  }

  private def updateValues(a: CachedAncillaryDatum, b: CachedAncillaryDatum): Unit = {
    require(a != null && b != null, "Null arguments are not allowed")
    a.altitude = b.altitude
    a.depthMeters = b.depthMeters
    a.crs = b.crs
    a.latitude = b.latitude
    a.longitude = b.longitude
    a.salinity = b.salinity
    a.temperatureCelsius = b.temperatureCelsius
    a.oxygenMlL = b.oxygenMlL
    a.pressureDbar = b.pressureDbar
    a.x = b.x
    a.y = b.y
    a.z = b.z
    a.posePositionUnits = b.posePositionUnits
    a.phi = b.phi
    a.theta = b.theta
    a.psi = b.psi
    a.lightTransmission = b.lightTransmission
  }
}

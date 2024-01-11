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

package org.mbari.annosaurus.controllers

import org.mbari.annosaurus.util.FastCollator
import java.time.Duration
import java.util.UUID

import org.mbari.annosaurus.repository.jpa.BaseDAO
import org.mbari.annosaurus.repository.{CachedAncillaryDatumDAO, NotFoundInDatastoreException}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import org.mbari.annosaurus.repository.jpa.entity.CachedAncillaryDatumEntity
import org.mbari.annosaurus.domain.CachedAncillaryDatum
import org.mbari.annosaurus.repository.jpa.entity.ImagedMomentEntity
import org.mbari.annosaurus.etc.jdk.Logging.given

/** @author
  *   Brian Schlining
  * @since 2017-05-01T10:53:00
  */
class CachedAncillaryDatumController(val daoFactory: JPADAOFactory)
    extends BaseController[CachedAncillaryDatumEntity, CachedAncillaryDatumDAO[
        CachedAncillaryDatumEntity
    ], CachedAncillaryDatum] {

    protected type ADDAO = CachedAncillaryDatumDAO[CachedAncillaryDatumEntity]
    private val log = System.getLogger(getClass.getName)

    override def newDAO(): CachedAncillaryDatumDAO[CachedAncillaryDatumEntity] =
        daoFactory.newCachedAncillaryDatumDAO()

    override def transform(a: CachedAncillaryDatumEntity): CachedAncillaryDatum =
        CachedAncillaryDatum.from(a, true)

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
    )(implicit ec: ExecutionContext): Future[Option[CachedAncillaryDatum]] = {

        def fn(dao: ADDAO): Option[CachedAncillaryDatumEntity] = {
            val imDao = daoFactory.newImagedMomentDAO(dao)
            imDao.findByUUID(imagedMomentUuid) match {
                case None               =>
                    log.atDebug.log(s"ImagedMoment with UUID of $imagedMomentUuid was no found")
                    None
                case Some(imagedMoment) =>
                    if (imagedMoment.getAncillaryDatum != null) {
                        log.atDebug.log(
                            s"ImagedMoment with UUID of $imagedMomentUuid already has ancillary data"
                        )
                        // TODO should this return the existing data?
                        None
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
                        imagedMoment.setAncillaryDatum(cad)
                        Some(cad)
                    }
            }
        }

        for
            entity <- exec(fn)
            dto <- findByImagedMomentUUID(imagedMomentUuid) if entity.isDefined
        yield dto
    }

    def create(imagedMomentUuid: UUID, datum: CachedAncillaryDatum)(implicit
        ec: ExecutionContext
    ): Future[Option[CachedAncillaryDatum]] = {
        def fn(dao: ADDAO): CachedAncillaryDatumEntity = {
            val imDao = daoFactory.newImagedMomentDAO(dao)
            imDao.findByUUID(imagedMomentUuid) match {
                case None               =>
                    throw new NotFoundInDatastoreException(
                        s"ImagedMoment with UUID of $imagedMomentUuid was no found"
                    )
                case Some(imagedMoment) =>
                    if (imagedMoment.getAncillaryDatum != null) {
                        throw new RuntimeException(
                            s"ImagedMoment with UUID of $imagedMomentUuid already has ancillary data"
                        )
                    }
                    else {
                        val entity = datum.toEntity
                        imagedMoment.setAncillaryDatum(entity)
                        entity
                    }
            }
        }

        for
            entity <- exec(fn)
            dto <- findByImagedMomentUUID(imagedMomentUuid)
        yield dto
    }

    def create(
        datum: CachedAncillaryDatum
    )(implicit ec: ExecutionContext): Future[Option[CachedAncillaryDatum]] =
        datum.imagedMomentUuid match {
            case None       =>
                Future.failed(
                    new RuntimeException(
                        s"ImagedMoment UUID is required but it was missing from $datum"
                    )
                )
            case Some(uuid) => create(uuid, datum)
        }

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
                    latitude.foreach(cad.setLatitude(_))
                    longitude.foreach(cad.setLongitude(_))
                    depthMeters.foreach(cad.setDepthMeters(_))
                    altitude.foreach(cad.setAltitude(_))
                    crs.foreach(cad.setCrs(_))
                    salinity.foreach(cad.setSalinity(_))
                    temperatureCelsius.foreach(cad.setTemperatureCelsius(_))
                    oxygenMlL.foreach(cad.setOxygenMlL(_))
                    pressureDbar.foreach(cad.setPressureDbar(_))
                    lightTransmission.foreach(cad.setLightTransmission(_))
                    x.foreach(cad.setX(_))
                    y.foreach(cad.setY(_))
                    z.foreach(cad.setZ(_))
                    posePositionUnits.foreach(cad.setPosePositionUnits(_))
                    phi.foreach(cad.setPhi(_))
                    theta.foreach(cad.setTheta(_))
                    psi.foreach(cad.setPsi(_))
                    cad
                })
                .map(transform)
        }

        exec(fn)
    }

    def findByVideoReferenceUUID(
        uuid: UUID
    )(implicit ec: ExecutionContext): Future[Seq[CachedAncillaryDatum]] = {
        def fn(dao: ADDAO): Seq[CachedAncillaryDatum] = {
            val imDao   = daoFactory.newImagedMomentDAO(dao)
            val moments = imDao.findByVideoReferenceUUID(uuid)
            moments
                .filter(_.getAncillaryDatum != null)
                .map(im => CachedAncillaryDatum.from(im.getAncillaryDatum, true))
                .toSeq
        }

        exec(fn)
    }

    def findByObservationUUID(
        uuid: UUID
    )(implicit ec: ExecutionContext): Future[Option[CachedAncillaryDatum]] = {
        def fn(dao: ADDAO): Option[CachedAncillaryDatum] =
            dao.findByObservationUUID(uuid).map(transform)

        exec(fn)
    }

    def findByImagedMomentUUID(
        uuid: UUID
    )(implicit ec: ExecutionContext): Future[Option[CachedAncillaryDatum]] = {
        def fn(dao: ADDAO): Option[CachedAncillaryDatum] =
            dao.findByImagedMomentUUID(uuid).map(transform)

        exec(fn)
    }

    def bulkCreateOrUpdate(
        data: Seq[CachedAncillaryDatum]
    )(implicit ec: ExecutionContext): Future[Seq[CachedAncillaryDatum]] = {
        def fn(dao: ADDAO): Seq[CachedAncillaryDatum] = {
            val fastDao = new FastAncillaryDataController(
                dao.asInstanceOf[BaseDAO[_]].entityManager
            )
            fastDao.createOrUpdate(data)
            data
        }

        exec(fn)
    }

    /** This method should be called within a transaction!
      *
      * @param d
      *   This MUST be a persistable object! (Not a CahcedAncillaryDatumBean)
      * @param im
      *   The moment whose ancillary data is being updated
      * @return
      *   The CachedAncillaryDatum.
      */
    private def createOrUpdate(
        d: CachedAncillaryDatumEntity,
        im: ImagedMomentEntity
    ): CachedAncillaryDatumEntity = {
        require(d != null, "A null CachedAncillaryDatum argument is not allowed")
        require(im != null, "A null ImagedMoment argument is not allowed")
        require(
            im.getUuid != null,
            "The ImagedMoment should already be present in the database. (Null UUID was found"
        )

        if (im.getAncillaryDatum != null) {
            updateValues(im.getAncillaryDatum, d)
            im.getAncillaryDatum
        }
        else {
            im.setAncillaryDatum(d)
            im.getAncillaryDatum
        }
    }

    def merge(
        data: Iterable[CachedAncillaryDatum],
        videoReferenceUuid: UUID,
        tolerance: Duration = Duration.ofMillis(7500)
    )(implicit ec: ExecutionContext): Future[Seq[CachedAncillaryDatum]] = {

        def fn(dao: ADDAO): Seq[CachedAncillaryDatum] = {
            val imDao         = daoFactory.newImagedMomentDAO(dao)
            val imagedMoments = imDao
                .findByVideoReferenceUUID(videoReferenceUuid)
                .filter(ir => ir.getRecordedTimestamp != null)

            val usefulData = data.filter(_.recordedTimestamp.isDefined)

            def imagedMomentToMillis(im: ImagedMomentEntity) =
                im.getRecordedTimestamp.toEpochMilli.toDouble

            def datumToMillis(cd: CachedAncillaryDatum) =
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
                val d = dao.newPersistentObject(cad.toEntity)
                transform(createOrUpdate(d, im))
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

    private def updateValues(
        a: CachedAncillaryDatumEntity,
        b: CachedAncillaryDatumEntity
    ): Unit = {
        require(a != null && b != null, "Null arguments are not allowed")
        a.setLatitude(b.getLatitude)
        a.setLongitude(b.getLongitude)
        a.setDepthMeters(b.getDepthMeters)
        a.setAltitude(b.getAltitude)
        a.setCrs(b.getCrs)
        a.setSalinity(b.getSalinity)
        a.setTemperatureCelsius(b.getTemperatureCelsius)
        a.setOxygenMlL(b.getOxygenMlL)
        a.setPressureDbar(b.getPressureDbar)
        a.setLightTransmission(b.getLightTransmission)
        a.setX(b.getX)
        a.setY(b.getY)
        a.setZ(b.getZ)
        a.setPosePositionUnits(b.getPosePositionUnits)
        a.setPhi(b.getPhi)
        a.setTheta(b.getTheta)
        a.setPsi(b.getPsi)
    }
}

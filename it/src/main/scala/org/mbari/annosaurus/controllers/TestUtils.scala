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

import org.mbari.annosaurus.etc.jdk.Strings
import org.mbari.annosaurus.repository.jpa.entity.{
    AssociationEntity,
    CachedAncillaryDatumEntity,
    ImageReferenceEntity,
    ImagedMomentEntity,
    ObservationEntity
}
import org.mbari.scilube3.ocean.Ocean
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import org.mbari.annosaurus.repository.jpa.entity._
import org.mbari.vcr4j.time.{FrameRates, Timecode}
import org.mbari.annosaurus.etc.jdk.Logging.given

import java.net.URI
import java.security.MessageDigest
import java.sql.Connection
import java.time.{Duration, Instant}
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration
import scala.util.Random
import scala.concurrent.ExecutionContext
import java.sql.Timestamp
import scala.concurrent.Await

object TestUtils {

    val Timeout            = duration.Duration(3, TimeUnit.SECONDS)
    val Digest             = MessageDigest.getInstance("SHA-512")
    private val random     = Random
    given ExecutionContext = ExecutionContext.global

    private val log = System.getLogger(getClass.getName)

    def runDdl(ddl: String, connection: Connection): Unit = {
        val statement = connection.createStatement();
        ddl
            .split(";")
            .foreach(sql => {
                log.atWarn.log(s"Running:\n$sql")
                statement.execute(sql)
            })
        statement.close()
    }

    def build(
        nImagedMoments: Int = 1,
        nObservations: Int = 0,
        nAssociations: Int = 0,
        nImageReferences: Int = 0,
        includeData: Boolean = false
    ): Seq[ImagedMomentEntity] = {
        val startDate          = Instant.now()
        val videoReferenceUuid = UUID.randomUUID()
        for (_ <- 0 until nImagedMoments)
            yield randomImagedMoment(
                nObservations,
                nAssociations,
                nImageReferences,
                includeData,
                videoReferenceUuid,
                startDate
            )
    }

    def create(
        entities: Seq[ImagedMomentEntity]
    )(using daoFactory: JPADAOFactory, ec: ExecutionContext): Seq[ImagedMomentEntity] = {
        log.atDebug.log(s"Creating ${entities.size} ImagedMoments: $entities")
        val dao = daoFactory.newImagedMomentDAO()
        Await.ready(dao.runTransaction(d => entities.foreach(x => d.create(x))), Timeout)
        dao.close()
        entities
    }

    def create(
        nImagedMoments: Int = 1,
        nObservations: Int = 0,
        nAssociations: Int = 0,
        nImageReferences: Int = 0,
        includeData: Boolean = false
    )(using daoFactory: JPADAOFactory, ec: ExecutionContext): Seq[ImagedMomentEntity] = {
        val xs  = build(nImagedMoments, nObservations, nAssociations, nImageReferences, includeData)
        log.atDebug.log(s"Creating ${xs.size} ImagedMoments: $xs")
        val dao = daoFactory.newImagedMomentDAO()
        Await.ready(dao.runTransaction(d => xs.foreach(x => d.create(x))), Timeout)
        // for (x <- xs)
        // do
        //     val f = dao.runTransaction(d => d.create(x))
        //     Await.result(f, Timeout)
        dao.close()
        xs
    }

    def randomImagedMoment(
        nObservations: Int = 0,
        nAssociations: Int = 0,
        nImageReferences: Int = 0,
        includeData: Boolean = false,
        videoReferenceUuid: UUID = UUID.randomUUID(),
        startDate: Instant = Instant.now()
    ): ImagedMomentEntity = {
        val et           = random.nextInt()
        val elapsedTime  = Duration.ofMillis(et)
        val recordedDate = Some(startDate.plusMillis(et)).orNull
        val timecode     =
            if (random.nextBoolean())
                Some(new Timecode(random.nextInt(10000).toDouble, FrameRates.NTSC))
            else None
        val imagedMoment =
            ImagedMomentEntity(videoReferenceUuid, recordedDate, timecode.orNull, elapsedTime)
        for (_ <- 0 until nObservations)
            imagedMoment.addObservation(randomObservation(nAssociations))
        for (_ <- 0 until nImageReferences) imagedMoment.addImageReference(randomImageReference())
        if (includeData) imagedMoment.setAncillaryDatum(randomData())
        imagedMoment
    }

    def randomObservation(nAssociations: Int = 0): ObservationEntity = {
        //        val obs = obsDao.newPersistentObject(Strings.random(conceptLength), Strings.random(10), Instant.now(), Some(Strings.random(6)))
        val concept         = Strings.random(random.nextInt(128))
        val duration        =
            if (random.nextBoolean()) Some(Duration.ofMillis(random.nextInt(5000))) else None
        val observationDate = Instant.now()
        val observer        = Some(Strings.random(32))
        val group           = if (random.nextBoolean()) Some(Strings.random(32)) else None
        val activity        = if (random.nextBoolean()) Some(Strings.random(32)) else None
        val obs             = ObservationEntity(
            concept,
            duration.orNull,
            observationDate,
            observer.orNull,
            group.orNull,
            activity.orNull
        )
        for (_ <- 0 until nAssociations) {
            obs.addAssociation(randomAssociation())
        }
        obs
    }

    def randomAssociation(): AssociationEntity = {
        val linkName  = Strings.random(random.nextInt(30) + 2)
        val linkValue = Strings.random(random.nextInt(254) + 2)
        val toConcept = Strings.random(random.nextInt(30) + 2)
        val mimeType  = Strings.random(random.nextInt(12))
        AssociationEntity(linkName, toConcept, linkValue, mimeType)
    }

    def randomImageReference(): ImageReferenceEntity = {
        val url         = URI
            .create(s"http://www.foo.bar/${Strings.random(10)}/image_${random.nextInt()}.png")
            .toURL
        val description = Strings.random(128)
        val width       = random.nextInt(1440) + 480
        val height      = math.round(width * 0.5625).toInt
        new ImageReferenceEntity(url, width, height, "image/png", description)
    }

    def randomData(): CachedAncillaryDatumEntity = {
        val lat      = (random.nextInt(18000) / 100d) - 90.0
        val lon      = (random.nextInt(36000) / 100d) - 180.0
        val pressure = random.nextInt(20000) / 10d
        val depth    = Ocean.depth(pressure, lat).floatValue
        val salinity = random.nextInt(3600) / 100f
        val temp     = random.nextInt(1500) / 100f
        val oxygen   = random.nextDouble() * 10f
        val crs      = "EPSG:4326"
        val x        = random.nextInt(1000) * 1d
        val y        = random.nextInt(1000) * 1d
        val z        = random.nextInt(1000) * 1d
        val pose     = "XYZ"
        val phi      = random.nextInt(36000) / 100d
        val theta    = random.nextInt(36000) / 100d
        val psi      = random.nextInt(36000) / 100d
        val trans    = random.nextInt(100) * 1f
        val datum    = new CachedAncillaryDatumEntity()
        datum.setAltitude(null)
        datum.setLatitude(lat)
        datum.setLongitude(lon)
        datum.setDepthMeters(depth)
        datum.setPressureDbar(pressure.toFloat)
        datum.setSalinity(salinity)
        datum.setTemperatureCelsius(temp)
        datum.setOxygenMlL(oxygen.toFloat)
        datum.setCrs(crs)
        datum.setX(x)
        datum.setY(y)
        datum.setZ(z)
        datum.setPosePositionUnits(pose)
        datum.setPhi(phi)
        datum.setTheta(theta)
        datum.setPsi(psi)
        datum.setLightTransmission(trans)
        datum
    }

    def randomVideoReferenceInfo(
        videoReferenceUuid: UUID = UUID.randomUUID()
    ): CachedVideoReferenceInfoEntity = {

        CachedVideoReferenceInfoEntity(
            videoReferenceUuid,
            "p-" + Strings.random(10),
            "missionId" + Strings.random(10),
            "missionContact" + Strings.random(10)
        )
    }



}

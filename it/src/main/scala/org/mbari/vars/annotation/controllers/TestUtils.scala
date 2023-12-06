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

import org.mbari.scilube3.ocean.Ocean
import org.slf4j.LoggerFactory

import java.sql.Connection
import scala.concurrent.duration
import java.util.concurrent.TimeUnit
import java.security.MessageDigest
import scala.util.Random
import org.mbari.vars.annotation.dao.jpa.{
    AssociationImpl,
    CachedAncillaryDatumImpl,
    ImageReferenceImpl,
    ImagedMomentImpl,
    JPADAOFactory,
    ObservationImpl
}

import java.util.UUID
import java.time.Instant
import java.time.Duration
import org.mbari.vars.annotation.etc.jdk.Strings
import org.mbari.vcr4j.time.{FrameRates, Timecode}

import java.net.URI

object TestUtils {

    val Timeout        = duration.Duration(3, TimeUnit.SECONDS)
    val Digest         = MessageDigest.getInstance("SHA-512")
    private val random = Random

    private val log = LoggerFactory.getLogger(getClass)

    def runDdl(ddl: String, connection: Connection): Unit = {
        val statement = connection.createStatement();
        ddl
            .split(";")
            .foreach(sql => {
                log.warn(s"Running:\n$sql")
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
    ): Seq[ImagedMomentImpl] = {
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
                 nImagedMoments: Int = 1,
                 nObservations: Int = 0,
                 nAssociations: Int = 0,
                 nImageReferences: Int = 0,
                 includeData: Boolean = false
             )(implicit daoFactory: JPADAOFactory): Seq[ImagedMomentImpl] = {
        val dao = daoFactory.newImagedMomentDAO()
        val xs = build(nImagedMoments, nObservations, nAssociations, nImageReferences, includeData)
        xs.foreach(e => dao.create(e))
        xs
    }

    def randomImagedMoment(
        nObservations: Int = 0,
        nAssociations: Int = 0,
        nImageReferences: Int = 0,
        includeData: Boolean = false,
        videoReferenceUuid: UUID = UUID.randomUUID(),
        startDate: Instant = Instant.now()
    ): ImagedMomentImpl = {
        val et           = random.nextInt()
        val elapsedTime  = Duration.ofMillis(et)
        val recordedDate = Some(startDate.plusMillis(et))
        val timecode     =
            if (random.nextBoolean())
                Some(new Timecode(random.nextInt(10000).toDouble, FrameRates.NTSC))
            else None
        val imagedMoment =
            ImagedMomentImpl(Some(videoReferenceUuid), recordedDate, timecode, Some(elapsedTime))
        for (_ <- 0 until nObservations)
            imagedMoment.addObservation(randomObservation(nAssociations))
        for (_ <- 0 until nImageReferences) imagedMoment.addImageReference(randomImageReference())
        if (includeData) imagedMoment.ancillaryDatum = randomData()
        imagedMoment
    }

    def randomObservation(nAssociations: Int = 0): ObservationImpl = {
        //        val obs = obsDao.newPersistentObject(Strings.random(conceptLength), Strings.random(10), Instant.now(), Some(Strings.random(6)))
        val concept         = Strings.random(random.nextInt(128))
        val duration        =
            if (random.nextBoolean()) Some(Duration.ofMillis(random.nextInt(5000))) else None
        val observationDate = Some(Instant.now())
        val observer        = if (random.nextBoolean()) Some(Strings.random(32)) else None
        val group           = if (random.nextBoolean()) Some(Strings.random(32)) else None
        val activity        = if (random.nextBoolean()) Some(Strings.random(32)) else None
        val obs             = ObservationImpl(concept, duration, observationDate, observer, group, activity)
        for (_ <- 0 until nAssociations) {
            obs.addAssociation(randomAssociation())
        }
        obs
    }

    def randomAssociation(): AssociationImpl = {
        val linkName  = Strings.random(random.nextInt(30) + 2)
        val linkValue = Strings.random(random.nextInt(254) + 2)
        val toConcept = Strings.random(random.nextInt(30) + 2)
        AssociationImpl(linkName, toConcept, linkValue)
    }

    def randomImageReference(): ImageReferenceImpl = {
        val url         = URI
            .create(s"http://www.foo.bar/${Strings.random(10)}/image_${random.nextInt}.png")
            .toURL
        val description = Strings.random(128)
        val width       = random.nextInt(1440) + 480
        val height      = math.round(width * 0.5625).toInt
        ImageReferenceImpl(url, Some(width), Some(height), Some("image/png"), Some(description))
    }

    def randomData(): CachedAncillaryDatumImpl = {
        val lat      = (random.nextInt(18000) / 100d) - 90.0
        val lon      = (random.nextInt(36000) / 100d) - 180.0
        val pressure = random.nextInt(20000) / 10f
        val depth    = Ocean.depth(pressure.toDouble, lat).toFloat
        val salinity = random.nextInt(3600) / 100f
        val temp     = random.nextInt(1500) / 100f
        val oxygen   = random.nextFloat()
        CachedAncillaryDatumImpl(lat, lon, depth, salinity, temp, pressure, oxygen)
    }

}

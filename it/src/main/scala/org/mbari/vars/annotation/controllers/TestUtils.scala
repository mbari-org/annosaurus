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

import org.slf4j.LoggerFactory

import java.sql.Connection
import scala.io.Source
import scala.concurrent.duration
import java.util.concurrent.TimeUnit
import java.security.MessageDigest
import scala.util.Random
import org.mbari.vars.annotation.dao.jpa.JPADAOFactory
import java.util.UUID
import java.time.Instant
import java.time.Duration
import org.mbari.vars.annotation.model.ImagedMoment
import org.mbari.vars.annotation.etc.jdk.Strings
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

    def build(numIm: Int, numObs: Int, numAssoc: Int, numIr: Int, data: Boolean = false)(implicit
        daoFactory: JPADAOFactory
    ): Seq[ImagedMoment] = {
        val imDao              = daoFactory.newImagedMomentDAO()
        val obsDao             = daoFactory.newObservationDAO(imDao)
        val assDao             = daoFactory.newAssociationDAO(imDao)
        val dataDao            = daoFactory.newCachedAncillaryDatumDAO(imDao)
        val irDao              = daoFactory.newImageReferenceDAO(imDao)
        val dDao = daoFactory.newCachedAncillaryDatumDAO(imDao)
        val videoReferenceUuid = UUID.randomUUID()
        val startDate          = Instant.now()
        val xs = for (imIdx <- 0 until numIm) yield {
            val et           = random.nextInt()
            val elaspedTime  = Duration.ofMillis(et)
            val recordedDate = startDate.plusMillis(et)
            val im           = imDao.newPersistentObject(
                videoReferenceUuid,
                None,
                Some(elaspedTime),
                Some(recordedDate)
            )
            val conceptLength = random.nextInt(30) + 2
            for (obsIdx <- 0 until numObs) {
                val obs = obsDao.newPersistentObject(Strings.random(conceptLength), Strings.random(10), Instant.now(), Some(Strings.random(6)))
                im.addObservation(obs)
                for (assocIdx <- 0 until numAssoc) {
                  val linkName = Strings.random(random.nextInt(30) + 2)
                  val linkValue = Strings.random(random.nextInt(254) + 2)
                  val toConcept = Strings.random(random.nextInt(30) + 2)
                  val ass = assDao.newPersistentObject(linkName, Some(toConcept), Some(linkValue))
                  obs.addAssociation(ass)
                }
            }
            for (irIdx <- 0 until numIr) {
              val url = URI.create(s"http://www.foo.bar/${Strings.random(10)}/image_$irIdx.png").toURL
              val description = Strings.random(128)
              val width = random.nextInt(1440) + 480
              val height = math.round(width * 0.5625).toInt
              val ir = irDao.newPersistentObject(url, Some(description), Some(width), Some(height), Some("image/png"))
              im.addImageReference(ir)
            }
            if (data) {
              // TODO generate a complete 
              val d = dDao.newPersistentObject(random.nextDouble(), random.nextDouble(), random.nextDouble())

            }

            im
        } 

        imDao.close();
        xs
    }

}

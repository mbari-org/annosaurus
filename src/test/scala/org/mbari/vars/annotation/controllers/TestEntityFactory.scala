package org.mbari.vars.annotation.controllers

import java.net.URL
import java.time.Instant
import java.util.UUID

import org.mbari.vars.annotation.dao.jpa.JPADAOFactory
import org.mbari.vars.annotation.model.ImagedMoment

import scala.util.Random

/**
 * @author Brian Schlining
 * @since 2019-10-22T11:51:00
 */
class TestEntityFactory(daoFactory: JPADAOFactory) {

  def createImagedMoment(n: Int,
                                 videoReferenceUuid: UUID = UUID.randomUUID(),
                                 concept: String = "foo",
                                 recordedTimestamp: Instant = Instant.now()): ImagedMoment = {

    val imDao = daoFactory.newImagedMomentDAO()
    val obsDao = daoFactory.newObservationDAO(imDao)
    val assDao = daoFactory.newAssociationDAO(imDao)
    val dataDao = daoFactory.newCachedAncillaryDatumDAO(imDao)
    val irDao = daoFactory.newImageReferenceDAO(imDao)
    val imagedMoment = imDao.newPersistentObject(videoReferenceUuid, recordedDate = Some(recordedTimestamp))
    for (i <- 0 until n) {
      val observation = obsDao.newPersistentObject(concept, "brian")
      imagedMoment.addObservation(observation)
      for (j <- 0 until n) {
        val association = assDao.newPersistentObject("test" + j, linkValue = Some(concept + " " + i))
        observation.addAssociation(association)
      }

      val imageReference = irDao.newPersistentObject(new URL("http://www.mbari.org/path/name/" + concept + "/" + i))
      imagedMoment.addImageReference(imageReference)
    }

    val data = dataDao.newPersistentObject(Random.nextDouble() * 90,
      Random.nextDouble() * 180,
      Random.nextDouble() * 2000)

    imagedMoment.ancillaryDatum = data
    imDao.close()
    imagedMoment

  }

}


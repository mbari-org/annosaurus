package org.mbari.vars.annotation.controllers

import java.util.UUID

import org.mbari.vars.annotation.dao.{AssociationDAO, NotFoundInDatastoreException}
import org.mbari.vars.annotation.model.Association

import scala.concurrent.Future

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-07-09T15:51:00
  */
class AssociationController(daoFactory: BasicDAOFactory)
    extends BaseController[Association, AssociationDAO[Association]] {


  type ADAO = AssociationDAO[Association]

  def create(observationUUID: UUID,
             linkName: String,
             toConcept: String,
             linkValue: String): Future[Association] = {
    def fn(dao: ADAO): Association = {
      val obsDao = daoFactory.newObservationDAO(dao)
      obsDao.findByUUID(observationUUID) match {
        case None => throw new NotFoundInDatastoreException(s"Observation with UUID of $observationUUID not found")
        case Some(observation) =>
          val association = dao.newPersistentObject()
          association.linkName = linkName
          association.toConcept = toConcept
          association.linkValue = linkValue
          observation.addAssociation(association)
          association
      }
    }
    exec(fn)
  }

  def update(uuid: UUID,
                observationUUID: Option[UUID] = None,
             linkName: Option[String] = None,
             toConcept: Option[String] = None,
             linkValue: Option[String] = None): Future[Option[Association]] = {

    def fn(dao: ADAO): Future[Option[Association]] = {
      dao.findByUUID(uuid) match {
        case None => throw new NotFoundInDatastoreException(s"Association with UUID of $uuid not found")
        case Some(association) =>
          linkName.foreach(association.linkName = _)
          toConcept.foreach(association.toConcept = _)
          linkValue.foreach(association.linkValue = _)
          // TODO move to new observation if it exists
          observationUUID.foreach(obsUUID => {

          })
          association
      }
    }

  }

  def findByLinkName(linkName: String): Future[Iterable[Association]]


}

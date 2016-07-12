package org.mbari.vars.annotation.controllers

import java.util.UUID

import org.mbari.vars.annotation.dao.{ AssociationDAO, NotFoundInDatastoreException }
import org.mbari.vars.annotation.model.Association

import scala.concurrent.{ ExecutionContext, Future }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-07-09T15:51:00
 */
class AssociationController(val daoFactory: BasicDAOFactory)
    extends BaseController[Association, AssociationDAO[Association]] {

  type ADAO = AssociationDAO[Association]

  override def newDAO(): AssociationDAO[Association] = daoFactory.newAssociationDAO()

  def create(
    observationUUID: UUID,
    linkName: String,
    toConcept: String,
    linkValue: String
  )(implicit ec: ExecutionContext): Future[Association] = {
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

  def update(
    uuid: UUID,
    observationUUID: Option[UUID] = None,
    linkName: Option[String] = None,
    toConcept: Option[String] = None,
    linkValue: Option[String] = None
  )(implicit ec: ExecutionContext): Future[Option[Association]] = {

    def fn(dao: ADAO): Option[Association] = {
      dao.findByUUID(uuid).map(association => {
        linkName.foreach(association.linkName = _)
        toConcept.foreach(association.toConcept = _)
        linkValue.foreach(association.linkValue = _)
        // Move to new observation if it exists
        for {
          obsUUID <- observationUUID
          obsDao = daoFactory.newObservationDAO(dao)
          obs <- obsDao.findByUUID(obsUUID)
        } {
          association.observation.removeAssociation(association)
          obs.addAssociation(association)
        }

        association
      })
    }

    exec(fn)
  }

  def findByLinkName(linkName: String)(implicit ec: ExecutionContext): Future[Iterable[Association]] = {
    def fn(dao: ADAO): Iterable[Association] = dao.findByLinkName(linkName)
    exec(fn)
  }

}

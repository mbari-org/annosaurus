package org.mbari.vars.annotation.dao.jpa

import javax.persistence.{ EntityManager, EntityManagerFactory }

import org.mbari.vars.annotation.dao.{ DAO, ImagedMomentDAO, ObservationDAO, _ }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-25T17:27:00
 */
trait JPADAOFactory
    extends DAOFactory[ImagedMomentImpl, ObservationImpl, AssociationImpl, ImageReferenceImpl, CachedAncillaryDatumImpl, CachedVideoReferenceInfoImpl] {

  def entityManagerFactory: EntityManagerFactory

  private def extractEntityManager(dao: DAO[_]): EntityManager =
    dao.asInstanceOf[BaseDAO[_]].entityManager

  override def newAssociationDAO(): AssociationDAO[AssociationImpl] =
    new AssociationDAOImpl(entityManagerFactory.createEntityManager())

  override def newAssociationDAO(dao: DAO[_]): AssociationDAO[AssociationImpl] =
    new AssociationDAOImpl(extractEntityManager(dao))

  override def newCachedAncillaryDatumDAO(): CachedAncillaryDatumDAO[CachedAncillaryDatumImpl] =
    new CachedAncillaryDatumDAOImpl(entityManagerFactory.createEntityManager())

  override def newCachedAncillaryDatumDAO(dao: DAO[_]): CachedAncillaryDatumDAO[CachedAncillaryDatumImpl] =
    new CachedAncillaryDatumDAOImpl(extractEntityManager(dao))

  override def newObservationDAO(): ObservationDAO[ObservationImpl] =
    new ObservationDAOImpl(entityManagerFactory.createEntityManager())

  override def newObservationDAO(dao: DAO[_]): ObservationDAO[ObservationImpl] =
    new ObservationDAOImpl(extractEntityManager(dao))

  override def newCachedVideoReferenceInfoDAO(): CachedVideoReferenceInfoDAO[CachedVideoReferenceInfoImpl] =
    new CachedVideoReferenceInfoDAOImpl(entityManagerFactory.createEntityManager())

  override def newCachedVideoReferenceInfoDAO(dao: DAO[_]): CachedVideoReferenceInfoDAO[CachedVideoReferenceInfoImpl] =
    new CachedVideoReferenceInfoDAOImpl(extractEntityManager(dao))

  override def newImageReferenceDAO(): ImageReferenceDAO[ImageReferenceImpl] =
    new ImageReferenceDAOImpl(entityManagerFactory.createEntityManager())

  override def newImageReferenceDAO(dao: DAO[_]): ImageReferenceDAO[ImageReferenceImpl] =
    new ImageReferenceDAOImpl(extractEntityManager(dao))

  override def newImagedMomentDAO(): ImagedMomentDAO[ImagedMomentImpl] =
    new ImagedMomentDAOImpl(entityManagerFactory.createEntityManager())

  override def newImagedMomentDAO(dao: DAO[_]): ImagedMomentDAO[ImagedMomentImpl] =
    new ImagedMomentDAOImpl(extractEntityManager(dao))

}

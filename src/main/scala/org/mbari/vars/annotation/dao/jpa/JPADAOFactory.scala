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

package org.mbari.vars.annotation.dao.jpa

import javax.persistence.{ EntityManager, EntityManagerFactory }

import com.typesafe.config.ConfigFactory
import org.mbari.vars.annotation.dao.{ DAO, ImagedMomentDAO, ObservationDAO, _ }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-25T17:27:00
 */
trait JPADAOFactory
  extends DAOFactory[ImagedMomentImpl, ObservationImpl, AssociationImpl, ImageReferenceImpl, CachedAncillaryDatumImpl, CachedVideoReferenceInfoImpl, IndexImpl] {

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

  override def newIndexDAO(): IndexDAO[IndexImpl] = new IndexDAOImpl(entityManagerFactory.createEntityManager())

  override def newIndexDAO(dao: DAO[_]): IndexDAO[IndexImpl] = new IndexDAOImpl(extractEntityManager(dao))

  override def newImageReferenceDAO(): ImageReferenceDAO[ImageReferenceImpl] =
    new ImageReferenceDAOImpl(entityManagerFactory.createEntityManager())

  override def newImageReferenceDAO(dao: DAO[_]): ImageReferenceDAO[ImageReferenceImpl] =
    new ImageReferenceDAOImpl(extractEntityManager(dao))

  override def newImagedMomentDAO(): ImagedMomentDAO[ImagedMomentImpl] =
    new ImagedMomentDAOImpl(entityManagerFactory.createEntityManager())

  override def newImagedMomentDAO(dao: DAO[_]): ImagedMomentDAO[ImagedMomentImpl] =
    new ImagedMomentDAOImpl(extractEntityManager(dao))

}

class JPADAOFactoryImpl(val entityManagerFactory: EntityManagerFactory) extends JPADAOFactory

object JPADAOFactory extends JPADAOFactory {

  lazy val entityManagerFactory = {
    val config = ConfigFactory.load()
    val environment = config.getString("database.environment")
    val nodeName = if (environment.equalsIgnoreCase("production")) "org.mbari.vars.annotation.database.production"
    else "org.mbari.vars.annotation.database.development"

    EntityManagerFactories(nodeName)
  }
}
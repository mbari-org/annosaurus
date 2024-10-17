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

package org.mbari.annosaurus.repository.jpa

import com.typesafe.config.ConfigFactory
import jakarta.persistence.{EntityManager, EntityManagerFactory}
import org.mbari.annosaurus.repository.{
    AssociationDAO,
    CachedAncillaryDatumDAO,
    CachedVideoReferenceInfoDAO,
    DAO,
    ImageReferenceDAO,
    ImagedMomentDAO,
    IndexDAO,
    ObservationDAO
}
import org.mbari.annosaurus.repository.jpa.entity.*

/**
 * @author
 *   Brian Schlining
 * @since 2016-06-25T17:27:00
 */
trait JPADAOFactory:

    def entityManagerFactory: EntityManagerFactory

    private def extractEntityManager(dao: DAO[?]): EntityManager =
        dao.asInstanceOf[BaseDAO[?]].entityManager

    def newAssociationDAO(): AssociationDAOImpl =
        new AssociationDAOImpl(entityManagerFactory.createEntityManager())

    def newAssociationDAO(dao: DAO[?]): AssociationDAOImpl =
        new AssociationDAOImpl(extractEntityManager(dao))

    def newCachedAncillaryDatumDAO(): CachedAncillaryDatumDAOImpl =
        new CachedAncillaryDatumDAOImpl(entityManagerFactory.createEntityManager())

    def newCachedAncillaryDatumDAO(
        dao: DAO[?]
    ): CachedAncillaryDatumDAOImpl =
        new CachedAncillaryDatumDAOImpl(extractEntityManager(dao))

    def newObservationDAO(): ObservationDAOImpl =
        new ObservationDAOImpl(entityManagerFactory.createEntityManager())

    def newObservationDAO(dao: DAO[?]): ObservationDAOImpl =
        new ObservationDAOImpl(extractEntityManager(dao))

    def newCachedVideoReferenceInfoDAO(): CachedVideoReferenceInfoDAOImpl =
        new CachedVideoReferenceInfoDAOImpl(entityManagerFactory.createEntityManager())

    def newCachedVideoReferenceInfoDAO(
        dao: DAO[?]
    ): CachedVideoReferenceInfoDAOImpl =
        new CachedVideoReferenceInfoDAOImpl(extractEntityManager(dao))

    def newIndexDAO(): IndexDAOImpl =
        new IndexDAOImpl(entityManagerFactory.createEntityManager())

    def newIndexDAO(dao: DAO[?]): IndexDAOImpl =
        new IndexDAOImpl(extractEntityManager(dao))

    def newImageReferenceDAO(): ImageReferenceDAOImpl =
        new ImageReferenceDAOImpl(entityManagerFactory.createEntityManager())

    def newImageReferenceDAO(dao: DAO[?]): ImageReferenceDAOImpl =
        new ImageReferenceDAOImpl(extractEntityManager(dao))

    def newImagedMomentDAO(): ImagedMomentDAOImpl =
        new ImagedMomentDAOImpl(entityManagerFactory.createEntityManager())

    def newImagedMomentDAO(dao: DAO[?]): ImagedMomentDAOImpl =
        new ImagedMomentDAOImpl(extractEntityManager(dao))

object JPADAOFactory extends JPADAOFactory:

    lazy val entityManagerFactory = EntityManagerFactories("database")

class JPADAOFactoryImpl(val entityManagerFactory: EntityManagerFactory) extends JPADAOFactory

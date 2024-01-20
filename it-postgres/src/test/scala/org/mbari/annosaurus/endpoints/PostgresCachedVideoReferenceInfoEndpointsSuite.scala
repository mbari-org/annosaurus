package org.mbari.annosaurus.endpoints
import org.mbari.annosaurus.repository.jpa.{PostgresTestDAOFactory, TestDAOFactory}

class PostgresCachedVideoReferenceInfoEndpointsSuite extends CachedVideoReferenceInfoEndpointsSuite {

    override def daoFactory: TestDAOFactory = PostgresTestDAOFactory

}

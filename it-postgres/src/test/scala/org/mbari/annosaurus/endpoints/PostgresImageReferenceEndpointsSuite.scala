package org.mbari.annosaurus.endpoints
import org.mbari.annosaurus.repository.jpa.{PostgresTestDAOFactory, TestDAOFactory}

class PostgresImageReferenceEndpointsSuite extends ImageReferenceEndpointsSuite {

    override given daoFactory: TestDAOFactory = PostgresTestDAOFactory

}

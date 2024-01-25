package org.mbari.annosaurus.endpoints
import org.mbari.annosaurus.repository.jpa.{PostgresTestDAOFactory, TestDAOFactory}

class PostgresImageEndpointsSuite extends ImageEndpointsSuite {
    override given daoFactory: TestDAOFactory = PostgresTestDAOFactory
}

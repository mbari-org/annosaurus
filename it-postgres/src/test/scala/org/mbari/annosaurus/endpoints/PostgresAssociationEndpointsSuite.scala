package org.mbari.annosaurus.endpoints
import org.mbari.annosaurus.repository.jpa.{PostgresTestDAOFactory, TestDAOFactory}

class PostgresAssociationEndpointsSuite extends AssociationEndpointsSuite {

    override given daoFactory: TestDAOFactory = PostgresTestDAOFactory

}

package org.mbari.annosaurus.endpoints
import org.mbari.annosaurus.repository.jpa.{SqlServerTestDAOFactory, TestDAOFactory}

class SqlServerIndexEndpointsSuite extends IndexEndpointsITSuite {

    override given daoFactory: TestDAOFactory = SqlServerTestDAOFactory

}

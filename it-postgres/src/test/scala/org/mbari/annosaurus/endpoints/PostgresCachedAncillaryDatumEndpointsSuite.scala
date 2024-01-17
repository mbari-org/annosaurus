package org.mbari.annosaurus.endpoints
import org.mbari.annosaurus.repository.jpa.{PostgresTestDAOFactory, TestDAOFactory}

class PostgresCachedAncillaryDatumEndpointsSuite extends CachedAncillaryDatumEndpointsITSuite {

    override given daoFactory: TestDAOFactory = PostgresTestDAOFactory

}

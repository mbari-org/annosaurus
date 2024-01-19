package org.mbari.annosaurus.endpoints
import org.mbari.annosaurus.repository.jpa.{PostgresTestDAOFactory, TestDAOFactory}

class PostgrestObservationEndpointsSuite extends ObservationEndpointsITSuite {
    override given daoFactory: TestDAOFactory = PostgresTestDAOFactory

}

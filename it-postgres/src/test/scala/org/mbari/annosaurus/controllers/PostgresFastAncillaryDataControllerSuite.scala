package org.mbari.annosaurus.controllers
import org.mbari.annosaurus.repository.jpa.{PostgresTestDAOFactory, TestDAOFactory}

class PostgresFastAncillaryDataControllerSuite extends FastAncillaryDataControllerITSuite {
    override given daoFactory: TestDAOFactory = PostgresTestDAOFactory

}

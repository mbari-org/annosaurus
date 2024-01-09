package org.mbari.annosaurus.controllers
import org.mbari.annosaurus.repository.jpa.{PostgresTestDAOFactory, TestDAOFactory}

class PostgresImageControllerSuite extends ImageControllerITSuite {

    override given daoFactory: TestDAOFactory = PostgresTestDAOFactory

}

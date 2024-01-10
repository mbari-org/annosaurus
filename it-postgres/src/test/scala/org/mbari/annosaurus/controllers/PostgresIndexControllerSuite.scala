package org.mbari.annosaurus.controllers
import org.mbari.annosaurus.repository.jpa.{PostgresTestDAOFactory, TestDAOFactory}

class PostgresIndexControllerSuite extends IndexControllerITSuite {

    override given daoFactory: TestDAOFactory = PostgresTestDAOFactory

}

package org.mbari.annosaurus.controllers
import org.mbari.annosaurus.repository.jpa.{PostgresTestDAOFactory, TestDAOFactory}

class PostgresImageReferenceControllerSuite extends ImageReferenceControllerITSuite {

    override given daoFactory: TestDAOFactory = PostgresTestDAOFactory

}

package org.mbari.annosaurus.controllers
import org.mbari.annosaurus.repository.jpa.{PostgresqlTestDAOFactory, TestDAOFactory}

class PostgresImageReferenceControllerSuite extends ImageReferenceControllerITSuite {

    override given daoFactory: TestDAOFactory = PostgresqlTestDAOFactory

}

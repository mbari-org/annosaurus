package org.mbari.annosaurus.controllers
import org.mbari.annosaurus.repository.jpa.{PostgresTestDAOFactory, TestDAOFactory}

class PostgresTestUtilsSuite extends TestUtilsSuite {

    override given daoFactory: TestDAOFactory = PostgresTestDAOFactory

}

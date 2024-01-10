package org.mbari.annosaurus.controllers
import org.mbari.annosaurus.repository.jpa.{SqlServerTestDAOFactory, TestDAOFactory}

class SqlServerIndexControllerSuite extends IndexControllerITSuite {

    override given daoFactory: TestDAOFactory = SqlServerTestDAOFactory

}

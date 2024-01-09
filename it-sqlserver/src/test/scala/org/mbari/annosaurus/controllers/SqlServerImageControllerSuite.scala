package org.mbari.annosaurus.controllers
import org.mbari.annosaurus.repository.jpa.{SqlServerTestDAOFactory, TestDAOFactory}

class SqlServerImageControllerSuite extends ImageControllerITSuite {

    override given daoFactory: TestDAOFactory = SqlServerTestDAOFactory


}

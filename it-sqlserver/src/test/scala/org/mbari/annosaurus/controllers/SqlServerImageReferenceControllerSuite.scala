package org.mbari.annosaurus.controllers
import org.mbari.annosaurus.repository.jpa.{SqlServerTestDAOFactory, TestDAOFactory}

class SqlServerImageReferenceControllerSuite extends ImageReferenceControllerITSuite {

    override given daoFactory: TestDAOFactory = SqlServerTestDAOFactory

}

package org.mbari.annosaurus.controllers
import org.mbari.annosaurus.repository.jpa.{SqlServerTestDAOFactory, TestDAOFactory}

class SqlServerFastAncillaryDataControllerSuite extends FastAncillaryDataControllerITSuite {

    override given daoFactory: TestDAOFactory = SqlServerTestDAOFactory

}

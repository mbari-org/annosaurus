package org.mbari.annosaurus.controllers
import org.mbari.annosaurus.repository.jpa.{SqlServerTestDAOFactory, TestDAOFactory}

class SqlServerCachedAncillaryDatumControllerSuite extends CachedAncillaryDatumControllerITSuite  {

    override given daoFactory: TestDAOFactory = SqlServerTestDAOFactory

}

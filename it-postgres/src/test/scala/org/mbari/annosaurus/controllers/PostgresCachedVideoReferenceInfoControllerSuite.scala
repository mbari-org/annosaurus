package org.mbari.annosaurus.controllers
import org.mbari.annosaurus.repository.jpa.{PostgresTestDAOFactory, TestDAOFactory}

class PostgresCachedVideoReferenceInfoControllerSuite extends CachedVideoReferenceInfoControllerITSuite {

    override given daoFactory: TestDAOFactory = PostgresTestDAOFactory


}

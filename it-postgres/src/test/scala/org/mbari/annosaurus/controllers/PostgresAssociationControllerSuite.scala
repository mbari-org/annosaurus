package org.mbari.annosaurus.controllers

import org.mbari.annosaurus.repository.jpa.PostgresqlTestDAOFactory
import org.mbari.annosaurus.repository.jpa.TestDAOFactory

class PostgresAssociationControllerSuite extends AssociationControllerITSuite {
    given daoFactory: TestDAOFactory = PostgresqlTestDAOFactory
  
}

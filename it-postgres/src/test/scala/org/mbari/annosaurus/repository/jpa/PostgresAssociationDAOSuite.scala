package org.mbari.annosaurus.repository.jpa

class PostgresAssociationDAOSuite extends AssociationDAOITSuite {
    given daoFactory: TestDAOFactory = PostgresqlTestDAOFactory
  
}

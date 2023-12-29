package org.mbari.annosaurus.repository.jpa

class SqlServerAssocationDAOSuite extends AssociationDAOITSuite {
  given daoFactory: TestDAOFactory = SqlServerTestDAOFactory
  
}

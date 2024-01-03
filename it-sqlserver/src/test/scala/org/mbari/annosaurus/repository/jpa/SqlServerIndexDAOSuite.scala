package org.mbari.annosaurus.repository.jpa

class SqlServerIndexDAOSuite extends IndexDAOITSuite {
  given daoFactory: TestDAOFactory = SqlServerTestDAOFactory
  
}

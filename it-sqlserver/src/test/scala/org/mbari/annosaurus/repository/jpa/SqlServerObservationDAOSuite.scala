package org.mbari.annosaurus.repository.jpa

class SqlServerObservationDAOSuite extends ObservationDAOITSuite {
  given daoFactory: TestDAOFactory = SqlServerTestDAOFactory
  
}

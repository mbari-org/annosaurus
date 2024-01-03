package org.mbari.annosaurus.repository.jpa

class SqlServerImageReferenceDAOSuite extends ImageReferenceDAOITSuite {
  given daoFactory: TestDAOFactory = SqlServerTestDAOFactory
  
}

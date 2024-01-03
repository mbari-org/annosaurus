package org.mbari.annosaurus.repository.jpa

class SqlServerCachedVideoReferenceInfoDAOSuite extends CachedVideoReferenceInfoDAOITSuite {
    given daoFactory: TestDAOFactory = SqlServerTestDAOFactory
}

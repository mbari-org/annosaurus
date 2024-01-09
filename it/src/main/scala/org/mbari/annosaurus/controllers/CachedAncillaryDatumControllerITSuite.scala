package org.mbari.annosaurus.controllers

import org.mbari.annosaurus.repository.jpa.{BaseDAOSuite, JPADAOFactory}

trait CachedAncillaryDatumControllerITSuite extends BaseDAOSuite{

    given JPADAOFactory = daoFactory

    val controller = new CachedAncillaryDatumController(daoFactory)

    test("create using params") {}

    test("create using uuid + DTO") {}

    test("create ussing extended DTO") {}

    test("update using params") {}

    test("findByVideoReferenceUUID") {}

    test("findByObservationUUID") {}

    test("findByImagedMomentUUID") {}

    test("bulkCreateOrUpdate") {}

    test("merge") {}

    test("deleteByVideoReferenceUUID") {}









}

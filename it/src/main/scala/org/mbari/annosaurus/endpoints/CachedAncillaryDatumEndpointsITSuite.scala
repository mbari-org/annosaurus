package org.mbari.annosaurus.endpoints


import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import sttp.client3.*
import org.mbari.annosaurus.etc.sdk.Futures.*
import org.junit.Assert.*
import org.mbari.annosaurus.controllers.{CachedAncillaryDatumController, TestUtils}
import org.mbari.annosaurus.domain.{CachedAncillaryDatum, CachedAncillaryDatumSC}
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import sttp.model.StatusCode

trait CachedAncillaryDatumEndpointsITSuite extends EndpointsSuite {

    private val log = System.getLogger(getClass.getName)

    given JPADAOFactory = daoFactory

    given jwtService: JwtService = new JwtService("mbari", "foo", "bar")

    private lazy val controller = new CachedAncillaryDatumController(daoFactory)
    private lazy val endpoints = new CachedAncillaryDatumEndpoints(controller)

    test("findDataByUuid") {
        val im = TestUtils.create(1, includeData = true).head
        val d = im.getAncillaryDatum
        runGet(
            endpoints.findDataByUuidImpl,
            s"http://test.com/v1/ancillarydata/${d.getUuid}",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[CachedAncillaryDatumSC](response.body).toCamelCase
                val expected = CachedAncillaryDatum.from(d, true)
                assertEquals(obtained, expected)
            }
        )
    }

}

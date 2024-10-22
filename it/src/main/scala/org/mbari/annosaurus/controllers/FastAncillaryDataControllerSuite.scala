/*
 * Copyright 2017 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.annosaurus.controllers

import org.mbari.annosaurus.domain.CachedAncillaryDatum
import org.mbari.annosaurus.etc.circe.CirceCodecs.*
import org.mbari.annosaurus.repository.jpa.{BaseDAOSuite, JPADAOFactory}

import scala.concurrent.ExecutionContext

trait FastAncillaryDataControllerSuite extends BaseDAOSuite:

    given JPADAOFactory    = daoFactory
    given ExecutionContext = ExecutionContext.global
    private val log        = System.getLogger(getClass.getName)

    override def beforeAll(): Unit = daoFactory.beforeAll()

    override def afterAll(): Unit = daoFactory.afterAll()

    private def runTransaction[A](fn: FastAncillaryDataController => A): A =
        val dao        = daoFactory.newCachedAncillaryDatumDAO()
        val em         = dao.entityManager
        val controller = FastAncillaryDataController(em)
        em.getTransaction.begin()
        val result     = fn(controller)
        em.getTransaction.commit()
        dao.close()
        result

    test("createOrUpdate") {
        val xs    = TestUtils.create(2, includeData = true) ++ TestUtils.create(2)
        val pairs = xs
            .map(im =>
                val ad = TestUtils.randomData()
                im -> CachedAncillaryDatum.from(ad).copy(imagedMomentUuid = Option(im.getUuid))
            )
            .toMap

        // Make sure we set the im uuid correctly
        for (im, ad) <- pairs
        do assertEquals(im.getUuid, ad.imagedMomentUuid.get)

        // Make the magic happen
        runTransaction(controller => controller.createOrUpdate(pairs.values.toSeq))

        // did we get magic?
        for x <- pairs.keys
        do
            val imController = ImagedMomentController(daoFactory)
            exec(imController.findByUUID(x.getUuid)) match
                case None     =>
                    fail(s"Failed to find imagedMoment with uuid ${x.getUuid}")
                case Some(im) =>
                    assert(im.ancillaryData.isDefined)
//                    log.atWarn.log(im.stringify)
                    val actual    = im.ancillaryData.get
                    val expected  = pairs(x)
                    val corrected = expected.copy(
                        uuid = actual.uuid,
                        imagedMomentUuid = None,
                        lastUpdated = actual.lastUpdated
                    )
                    assertEquals(actual, corrected)
    }

    test("exists") {
        val im     = TestUtils.create(1, includeData = true).head
        val cad    = im.getAncillaryDatum
        val dto    = CachedAncillaryDatum.from(cad).copy(imagedMomentUuid = Some(im.getUuid))
        val exists = runTransaction(controller => controller.exists(dto))
        assert(exists)
    }

    test("create") {
        val im           = TestUtils.create(1).head
        val ad           = TestUtils.randomData()
        val expected     = CachedAncillaryDatum.from(ad).copy(imagedMomentUuid = Some(im.getUuid))
        val ok           = runTransaction(controller => controller.create(expected))
        assert(ok)
        val imController = ImagedMomentController(daoFactory)
        val opt          = exec(imController.findByUUID(im.getUuid))
        assert(opt.isDefined)
        val actual       = opt.get.ancillaryData.get

        // munge the expected and actual to make them comparable
        val expected2 = expected.copy(uuid = actual.uuid)
        val actual2   = actual.copy(imagedMomentUuid = Some(im.getUuid), lastUpdated = None)
        assertEquals(actual2, expected2)
    }

    test("update") {
        val im  = TestUtils.create(1, 1, 1, includeData = true).head
        val cad = im.getAncillaryDatum
        val dto = CachedAncillaryDatum
            .from(cad)
            .copy(imagedMomentUuid = Some(im.getUuid), salinity = Some(34))
        val ok  = runTransaction(controller => controller.update(dto))
        assert(ok)

        val imController = ImagedMomentController(daoFactory)
        val opt          = exec(imController.findByUUID(im.getUuid))
        opt match
            case None     =>
                fail(s"Failed to find imagedMoment with uuid ${im.getUuid}")
            case Some(im) =>
                val obtained = im.ancillaryData.get
                assertEquals(obtained.salinity, dto.salinity)

    }

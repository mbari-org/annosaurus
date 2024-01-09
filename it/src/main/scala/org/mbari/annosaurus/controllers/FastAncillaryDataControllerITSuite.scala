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
import org.mbari.annosaurus.repository.jpa.{BaseDAOSuite, JPADAOFactory}
import org.mbari.annosaurus.etc.jdk.Logging.given
import org.mbari.annosaurus.repository.jpa.extensions.runTransaction
import org.mbari.annosaurus.etc.circe.CirceCodecs.{given, *}

trait FastAncillaryDataControllerITSuite extends BaseDAOSuite {

    given JPADAOFactory         = daoFactory
    private val log             = System.getLogger(getClass.getName)
    private lazy val controller = FastAncillaryDataController(
        daoFactory.entityManagerFactory.createEntityManager()
    )

    override def beforeAll(): Unit = daoFactory.beforeAll()

    override def afterAll(): Unit = daoFactory.afterAll()

    test("createOrUpdate") {
        val xs = TestUtils.create(2, includeData = true) ++ TestUtils.create(2)
        val pairs = xs.map(im => {
            val ad = TestUtils.randomData()
            im -> CachedAncillaryDatum.from(ad).copy(imagedMomentUuid = Option(im.getUuid))
        }).toMap

        // Make sure we set the im uuid correctly
        for
            (im, ad) <- pairs
        do
            assertEquals(im.getUuid, ad.imagedMomentUuid.get)

        // Make the magic happen
        controller.createOrUpdate(pairs.values.toSeq)

        // did we get magic?
        for
            x <- pairs.keys
        do
            val imController = ImagedMomentController(daoFactory)
            exec(imController.findByUUID(x.getUuid)) match
                case None =>
                    fail(s"Failed to find imagedMoment with uuid ${x.getUuid}")
                case Some(im) =>
                    assert(im.ancillaryData.isDefined)
                    log.atWarn.log(im.stringify)
                    val actual = im.ancillaryData.get
                    val expected = pairs(x)
                    assertEquals(actual, expected)
    }

    test("createAsync") {}

    test("exists") {}

    test("create") {
        val im = TestUtils.create(1).head
        val ad = TestUtils.randomData()
        val expected = CachedAncillaryDatum.from(ad).copy(imagedMomentUuid = Some(im.getUuid))
        val ok = exec(controller.entityManager.runTransaction { em =>
            controller.create(expected)
        })
        assert(ok)
        val imController = ImagedMomentController(daoFactory)
        val opt = exec(imController.findByUUID(im.getUuid))
        assert(opt.isDefined)
        val actual = opt.get.ancillaryData.get

        // munge the expected and actual to make them comparable
        val expected2 = expected.copy(uuid = actual.uuid)
        val actual2 = actual.copy(imagedMomentUuid = Some(im.getUuid), lastUpdated = None)
        assertEquals(actual2, expected2)
    }

    test("update") {}

}

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

package org.mbari.annosaurus.repository.jpa

import org.mbari.annosaurus.AssertUtils
import org.mbari.annosaurus.controllers.TestUtils

import scala.jdk.CollectionConverters.*

trait LocalizationDAOSuite extends BaseDAOSuite:

    given JPADAOFactory = daoFactory

    test("findByObservationUuid") {
        val xs                           = TestUtils.create(1, 1, nLocalizations = 2)
        val o                            = xs.head.getObservations.asScala.head
        given dao: LocalizationDAOImpl   = daoFactory.newLocalizationDAO()
        val ys                           = run(() => dao.findByObservationUuid(o.getUuid))
        assertEquals(ys.size, 2)
        val expected                     = o.getLocalizations.asScala.toSeq.sortBy(_.getUuid)
        expected.zip(ys.sortBy(_.getUuid)).foreach((a, b) => AssertUtils.assertSameLocalization(a, b))
        dao.close()
    }

    test("findByObservationUuidAndType") {
        val xs                         = TestUtils.create(1, 1, nLocalizations = 2)
        val o                          = xs.head.getObservations.asScala.head
        val point                      = o.getLocalizations.asScala.find(_.getLocalizationType == "point").get
        given dao: LocalizationDAOImpl = daoFactory.newLocalizationDAO()
        val y                          = run(() => dao.findByObservationUuidAndType(o.getUuid, "point"))
        assert(y.isDefined)
        AssertUtils.assertSameLocalization(point, y.get)
        val z                          = run(() => dao.findByObservationUuidAndType(o.getUuid, "mask"))
        assert(z.isEmpty)
        dao.close()
    }

    test("findByVideoReferenceUuid") {
        val xs                         = TestUtils.create(1, 1, nLocalizations = 2)
        val im                         = xs.head
        given dao: LocalizationDAOImpl = daoFactory.newLocalizationDAO()
        val ys                         = run(() => dao.findByVideoReferenceUuid(im.getVideoReferenceUuid, None, None))
        assertEquals(ys.size, 2)
        val page                       = run(() => dao.findByVideoReferenceUuid(im.getVideoReferenceUuid, Some(1), Some(1)))
        assertEquals(page.size, 1)
        dao.close()
    }

    test("deleteByUUID") {
        val xs                         = TestUtils.create(1, 1, nLocalizations = 2)
        val o                          = xs.head.getObservations.asScala.head
        val a                          = o.getLocalizations.asScala.head
        given dao: LocalizationDAOImpl = daoFactory.newLocalizationDAO()
        run(() => dao.deleteByUUID(a.getUuid))
        assert(run(() => dao.findByUUID(a.getUuid)).isEmpty)
        assertEquals(run(() => dao.findByObservationUuid(o.getUuid)).size, 1)
        dao.close()
    }

    test("cascade delete with imaged moment") {
        val xs                         = TestUtils.create(1, 1, nLocalizations = 2)
        val o                          = xs.head.getObservations.asScala.head
        given dao: LocalizationDAOImpl = daoFactory.newLocalizationDAO()
        assertEquals(run(() => dao.findByObservationUuid(o.getUuid)).size, 2)
        daoFactory.cleanup()
        assert(run(() => dao.findAll()).isEmpty)
        dao.close()
    }

    test("all localization types persist") {
        val xs                         = TestUtils.create(1, 1, nLocalizations = 5)
        val o                          = xs.head.getObservations.asScala.head
        given dao: LocalizationDAOImpl = daoFactory.newLocalizationDAO()
        val ys                         = run(() => dao.findByObservationUuid(o.getUuid))
        assertEquals(ys.map(_.getLocalizationType).toSet, Set("bounding_box", "point", "line", "polygon", "mask"))
        val expected                   = o.getLocalizations.asScala.toSeq.sortBy(_.getUuid)
        expected.zip(ys.sortBy(_.getUuid)).foreach((a, b) => AssertUtils.assertSameLocalization(a, b))
        dao.close()
    }

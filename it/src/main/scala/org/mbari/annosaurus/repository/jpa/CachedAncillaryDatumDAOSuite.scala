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

import org.mbari.annosaurus.controllers.TestUtils
import org.mbari.annosaurus.AssertUtils
import org.mbari.annosaurus.domain.CachedAncillaryDatum

trait CachedAncillaryDatumDAOSuite extends BaseDAOSuite:
    given JPADAOFactory = daoFactory

    test("findByUUID") {
        val im                                 = TestUtils.create(1, 1, 1, 1, true).head
        val d                                  = im.getAncillaryDatum()
        given dao: CachedAncillaryDatumDAOImpl = daoFactory.newCachedAncillaryDatumDAO()
        val ys                                 = run(() => dao.findByUUID(d.getUuid()))
        dao.close()
        assert(ys.size == 1)
        AssertUtils.assertSameAncillaryDatum(ys.head, d)
    }

    test("deleteByUUID") {
        val im                                 = TestUtils.create(1, 1, 1, 1, true).head
        val d                                  = im.getAncillaryDatum()
        given dao: CachedAncillaryDatumDAOImpl = daoFactory.newCachedAncillaryDatumDAO()
        run(() => dao.deleteByUUID(d.getUuid()))
        val ys                                 = run(() => dao.findByUUID(d.getUuid()))
        dao.close()
        assert(ys.size == 0)
    }

    test("create") {
        val im                                 = TestUtils.create(1, 0, 0, 0, false).head
        given dao: CachedAncillaryDatumDAOImpl = daoFactory.newCachedAncillaryDatumDAO()
        val imDao                              = daoFactory.newImagedMomentDAO(dao)
        val d                                  = TestUtils.randomData()
        run(() =>
            imDao.findByUUID(im.getUuid()) match
                case Some(im0) =>
                    im0.setAncillaryDatum(d)
                // dao.create(d) // Not needed as adding to imagedmoment in transaction will persist the datum
                case None      => fail("Failed to find imaged moment")
        )
        run(() => dao.findByUUID(d.getUuid())) match
            case Some(d1) => AssertUtils.assertSameAncillaryDatum(d1, d)
            case None     => fail("Failed to find ancillary datum")
        dao.close()

    }

    test("update") {
        val im                                 = TestUtils.create(1, 1, 1, 1, true).head
        val d                                  = im.getAncillaryDatum()
        given dao: CachedAncillaryDatumDAOImpl = daoFactory.newCachedAncillaryDatumDAO()
        d.setAltitude(-1000)
        run(() => dao.update(d))
        val ys                                 = run(() => dao.findByUUID(d.getUuid()))
        dao.close()
        assert(ys.size == 1)
        AssertUtils.assertSameAncillaryDatum(ys.head, d)
    }

    test("delete") {
        val im                                 = TestUtils.create(1, 1, 1, 1, true).head
        val d                                  = im.getAncillaryDatum()
        given dao: CachedAncillaryDatumDAOImpl = daoFactory.newCachedAncillaryDatumDAO()
        run(() =>
            dao.findByUUID(d.getUuid()) match
                case Some(a) => dao.delete(a)
                case None    => fail("Failed to find ancillary datum")
        )
        val ys                                 = run(() => dao.findByUUID(d.getUuid()))
        dao.close()
        assert(ys.isEmpty)
    }

    test("findAll") {
        val im                                 = TestUtils.create(1, 1, 1, 1, true).head
        val d                                  = im.getAncillaryDatum()
        given dao: CachedAncillaryDatumDAOImpl = daoFactory.newCachedAncillaryDatumDAO()
        val ys                                 = run(() => dao.findAll())
        dao.close()
        assert(ys.size >= 1)
        val opt                                = ys.filter(_.getUuid() == d.getUuid()).headOption
        assert(opt.isDefined)
        AssertUtils.assertSameAncillaryDatum(opt.get, d)
    }

    test("findByObservationUuid") {
        val im                                 = TestUtils.create(1, 1, 1, 1, true).head
        val o                                  = im.getObservations().iterator().next()
        val d                                  = im.getAncillaryDatum()
        given dao: CachedAncillaryDatumDAOImpl = daoFactory.newCachedAncillaryDatumDAO()
        val opt                                = run(() => dao.findByObservationUUID(o.getUuid()))
        dao.close()
        assert(opt.isDefined)
        AssertUtils.assertSameAncillaryDatum(opt.get, d)
    }

    test("findDTOByObservationUuid") {
        val im = TestUtils.create(1, 1, 1, 1, true).head
        val o  = im.getObservations().iterator().next()
        val d  = im.getAncillaryDatum()

        given dao: CachedAncillaryDatumDAOImpl = daoFactory.newCachedAncillaryDatumDAO()

        val opt      = run(() => dao.findDTOByObservationUuid(o.getUuid()))
        dao.close()
        assert(opt.isDefined)
        val obtained = CachedAncillaryDatum.from(opt.get)
        val expected = CachedAncillaryDatum.from(d, true)
        assertEquals(obtained, expected)
    }

    test("findByImagedMomentUUID") {
        val im                                 = TestUtils.create(1, 1, 1, 1, true).head
        val d                                  = im.getAncillaryDatum()
        given dao: CachedAncillaryDatumDAOImpl = daoFactory.newCachedAncillaryDatumDAO()
        val ys                                 = run(() => dao.findByImagedMomentUUID(im.getUuid()))
        dao.close()
        assert(ys.size == 1)
        AssertUtils.assertSameAncillaryDatum(ys.head, d)
    }

    test("deleteByVideoReferenceUuid") {
        val im                                 = TestUtils.create(1, 1, 1, 1, true).head
        val d                                  = im.getAncillaryDatum()
        given dao: CachedAncillaryDatumDAOImpl = daoFactory.newCachedAncillaryDatumDAO()
        run(() => dao.deleteByVideoReferenceUuid(im.getVideoReferenceUuid()))
        val ys                                 = run(() => dao.findByUUID(d.getUuid()))
        dao.close()
        assert(ys.size == 0)
    }

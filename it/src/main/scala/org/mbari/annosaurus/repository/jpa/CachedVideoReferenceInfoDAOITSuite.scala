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
import org.mbari.annosaurus.domain.CachedVideoReferenceInfo
import org.mbari.annosaurus.repository.jpa.entity.CachedVideoReferenceInfoEntity

trait CachedVideoReferenceInfoDAOITSuite extends BaseDAOSuite {

    given JPADAOFactory = daoFactory

    test("create") {
        val vi = TestUtils.randomVideoReferenceInfo()
        given dao: CachedVideoReferenceInfoDAOImpl = daoFactory.newCachedVideoReferenceInfoDAO()
        run(() => dao.create(vi))
        dao.close()
        assert(vi.getUuid() != null)
    }

    def createTestData(): CachedVideoReferenceInfoEntity = {
        val vi = TestUtils.randomVideoReferenceInfo()
        given dao: CachedVideoReferenceInfoDAOImpl = daoFactory.newCachedVideoReferenceInfoDAO()
        run(() => dao.create(vi))
        dao.close()
        vi

    }
    test("update") {
        val vi = createTestData()
        given dao: CachedVideoReferenceInfoDAOImpl = daoFactory.newCachedVideoReferenceInfoDAO()
        vi.setMissionContact("newMissionContact")
        run(() => dao.update(vi))
        run(() => dao.findByUUID(vi.getUuid())) match
            case None => fail("should have found the entity")
            case Some(value) => AssertUtils.assertSameVideoReferenceInfo(value, vi)
        dao.close()
    }

    test("delete") {
        val vi = createTestData()
        given dao: CachedVideoReferenceInfoDAOImpl = daoFactory.newCachedVideoReferenceInfoDAO()
        run(() => {
            // update brings entity into transactional context
            dao.delete(dao.update(vi))
        })
        run(() => dao.findByUUID(vi.getUuid())) match
            case None => // ok
            case Some(value) => fail("should not have found the entity")
        dao.close()
    }

    test("findByUUID") {
        val vi = createTestData()
        given dao: CachedVideoReferenceInfoDAOImpl = daoFactory.newCachedVideoReferenceInfoDAO()
        run(() => dao.findByUUID(vi.getUuid())) match
            case None => fail("should have found the entity")
            case Some(value) => AssertUtils.assertSameVideoReferenceInfo(value, vi)
        dao.close()
    }

    test("deleteByUUID") {
        val vi = createTestData()
        given dao: CachedVideoReferenceInfoDAOImpl = daoFactory.newCachedVideoReferenceInfoDAO()
        run(() => dao.deleteByUUID(vi.getUuid()))
        run(() => dao.findByUUID(vi.getUuid())) match
            case None => // ok
            case Some(value) => fail("should not have found the entity")
    }
    
    test("findByMissionContact") {
        val vi = createTestData()
        given dao: CachedVideoReferenceInfoDAOImpl = daoFactory.newCachedVideoReferenceInfoDAO()
        run(() => dao.findByMissionContact(vi.getMissionContact())) match
            case Nil => fail("should have found the entity")
            case xs => AssertUtils.assertSameVideoReferenceInfo(xs.head, vi)
        dao.close()
    }

    test("findByPlatformName") {
        val vi = createTestData()
        given dao: CachedVideoReferenceInfoDAOImpl = daoFactory.newCachedVideoReferenceInfoDAO()
        run(() => dao.findByPlatformName(vi.getPlatformName())) match
            case Nil => fail("should have found the entity")
            case xs => AssertUtils.assertSameVideoReferenceInfo(xs.head, vi)
        dao.close()
    }

    test("findByMissionID") {
        val vi = createTestData()
        given dao: CachedVideoReferenceInfoDAOImpl = daoFactory.newCachedVideoReferenceInfoDAO()
        run(() => dao.findByMissionID(vi.getMissionId())) match
            case Nil => fail("should have found the entity")
            case xs => AssertUtils.assertSameVideoReferenceInfo(xs.head, vi)
        dao.close()
    }

    test("findByVideoReferenceUUID") {
        val vi = createTestData()
        given dao: CachedVideoReferenceInfoDAOImpl = daoFactory.newCachedVideoReferenceInfoDAO()
        run(() => dao.findByVideoReferenceUUID(vi.getVideoReferenceUuid())) match
            case None => fail("should have found the entity")
            case Some(value) => AssertUtils.assertSameVideoReferenceInfo(value, vi)
        dao.close()
    }

    test("findAll") {
        val vi = createTestData()
        given dao: CachedVideoReferenceInfoDAOImpl = daoFactory.newCachedVideoReferenceInfoDAO()
        val xs = run(() => dao.findAll())
        assert(xs.size >= 1)
        val opt = xs.filter(_.getUuid() == vi.getUuid()).headOption
        assert(opt.isDefined)
        AssertUtils.assertSameVideoReferenceInfo(opt.get, vi)
        dao.close()
    }

    test("findAllVideoReferenceUUIDs") {
        val vi = createTestData()
        given dao: CachedVideoReferenceInfoDAOImpl = daoFactory.newCachedVideoReferenceInfoDAO()
        val xs = run(() => dao.findAllVideoReferenceUUIDs()).toSeq
        assert(xs.size >= 1)
        assert(xs.contains(vi.getVideoReferenceUuid()))
        dao.close()
    }

    test("findAllMissionContacts") {
        val vi = createTestData()
        given dao: CachedVideoReferenceInfoDAOImpl = daoFactory.newCachedVideoReferenceInfoDAO()
        val xs = run(() => dao.findAllMissionContacts()).toSeq
        assert(xs.size >= 1)
        assert(xs.contains(vi.getMissionContact()))
        dao.close()
    }

    test("findAllPlatformNames") {
        val vi = createTestData()
        given dao: CachedVideoReferenceInfoDAOImpl = daoFactory.newCachedVideoReferenceInfoDAO()
        val xs = run(() => dao.findAllPlatformNames()).toSeq
        println(xs)
        println(vi.getPlatformName())
        assert(xs.size >= 1)
        assert(xs.contains(vi.getPlatformName()))
        dao.close()
    }

    test("findAllMissionIDs") {
        val vi = createTestData()
        given dao: CachedVideoReferenceInfoDAOImpl = daoFactory.newCachedVideoReferenceInfoDAO()
        val xs = run(() => dao.findAllMissionIDs()).toSeq
        assert(xs.size >= 1)
        assert(xs.contains(vi.getMissionId()))
        dao.close()
    }

}

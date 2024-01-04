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

import org.mbari.annosaurus.repository.jpa.entity.IndexEntity
import java.util.UUID
import org.mbari.vcr4j.time.Timecode
import org.mbari.annosaurus.controllers.TestUtils
import org.mbari.annosaurus.AssertUtils
import org.mbari.annosaurus.controllers.TestUtils.create

trait IndexDAOITSuite extends BaseDAOSuite {

    given JPADAOFactory = daoFactory

    // test("create") {
    //     val idx = new IndexEntity(TestUtils.randomImagedMoment())
    //     given dao: IndexDAOImpl = daoFactory.newIndexDAO()
    //     run(() => dao.create(idx))
    //     run(() => dao.findByUUID(idx.getUuid())) match
    //         case None => fail("should have found the entity")
    //         case Some(value) => AssertUtils.assertSameIndex(value, idx)
    //     dao.close()

    // }

    test("create") {
        intercept[Exception] {
            val idx                 = new IndexEntity(TestUtils.create().head)
            given dao: IndexDAOImpl = daoFactory.newIndexDAO()
            run(() => dao.create(idx))
            dao.close()
        }
    }

    test("update") {
        val idx                 = new IndexEntity(TestUtils.create().head)
        given dao: IndexDAOImpl = daoFactory.newIndexDAO()
        idx.setTimecode(new Timecode("00:11:22:33"))
        run(() => dao.update(idx))
        run(() => dao.findByUUID(idx.getUuid())) match
            case None        => fail("should have found the entity")
            case Some(value) => AssertUtils.assertSameIndex(value, idx)
        dao.close()
    }

    test("delete") {
        // This test should fail. The delete method is not implemented
        intercept[Exception] {
            val idx                 = new IndexEntity(TestUtils.create().head)
            given dao: IndexDAOImpl = daoFactory.newIndexDAO()
            run(() => dao.delete(idx))
            dao.close()
        }
    }

    test("deleteByUUID") {
        // This test should fail. The deleteByUUID method is not implemented
        intercept[Exception] {
            val idx                 = new IndexEntity(TestUtils.create().head)
            given dao: IndexDAOImpl = daoFactory.newIndexDAO()
            run(() => dao.deleteByUUID(idx.getUuid()))
            dao.close()
        }
    }

    // test("delete") {
    //     val idx = new IndexEntity(TestUtils.randomImagedMoment())
    //     given dao: IndexDAOImpl = daoFactory.newIndexDAO()
    //     run(() => dao.create(idx))
    //     val opt = run(() => dao.findByUUID(idx.getUuid()))
    //     assert(opt.isDefined)
    //     run(() => dao.delete(idx))
    //     val opt2 = run(() => dao.findByUUID(idx.getUuid()))
    //     assert(opt2.isEmpty)
    // }

    // test("deleteByUUID") {
    //     val idx = new IndexEntity(TestUtils.randomImagedMoment())
    //     given dao: IndexDAOImpl = daoFactory.newIndexDAO()
    //     run(() => dao.create(idx))
    //     val opt = run(() => dao.findByUUID(idx.getUuid()))
    //     assert(opt.isDefined)
    //     run(() => dao.deleteByUUID(idx.getUuid()))
    //     val opt2 = run(() => dao.findByUUID(idx.getUuid()))
    //     assert(opt2.isEmpty)
    // }

    test("findByUUID") {
        val idx                 = new IndexEntity(TestUtils.create().head)
        given dao: IndexDAOImpl = daoFactory.newIndexDAO()
        run(() => dao.findByUUID(idx.getUuid())) match
            case None        => fail("should have found the entity")
            case Some(value) => AssertUtils.assertSameIndex(value, idx)
        dao.close()
    }

    test("findAll") {
        intercept[Exception] {
            val idx                 = new IndexEntity(TestUtils.create().head)
            given dao: IndexDAOImpl = daoFactory.newIndexDAO()
            run(() => dao.findAll())
            dao.close()
        }
    }

    // test("findAll") {
    //     val idx = new IndexEntity(TestUtils.randomImagedMoment())
    //     given dao: IndexDAOImpl = daoFactory.newIndexDAO()
    //     run(() => dao.create(idx))
    //     val xs = run(() => dao.findAll())
    //     assert(xs.size >= 1)
    //     val opt = xs.filter(_.getUuid() == idx.getUuid()).headOption
    //     assert(opt.isDefined)
    //     AssertUtils.assertSameIndex(opt.get, idx)
    //     dao.close()
    // }

    test("findByVideoReferenceUuid") {
        val idx                 = new IndexEntity(TestUtils.create().head)
        given dao: IndexDAOImpl = daoFactory.newIndexDAO()
        val xs                  = run(() => dao.findByVideoReferenceUuid(idx.getVideoReferenceUuid()))
        assert(xs.size >= 1)
        val opt                 = xs.filter(_.getUuid() == idx.getUuid()).headOption
        assert(opt.isDefined)
        AssertUtils.assertSameIndex(opt.get, idx)
        dao.close()
    }

}

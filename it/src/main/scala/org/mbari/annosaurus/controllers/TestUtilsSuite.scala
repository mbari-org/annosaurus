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

import org.mbari.annosaurus.AssertUtils
import org.mbari.annosaurus.repository.jpa.TestDAOFactory

import java.util.concurrent.TimeUnit
import scala.concurrent.{Await, ExecutionContext}
import scala.jdk.CollectionConverters.*
import org.mbari.annosaurus.repository.jpa.BaseDAOSuite

trait TestUtilsSuite extends BaseDAOSuite {

    val timeout = scala.concurrent.duration.Duration(10, TimeUnit.SECONDS)

    test("build") {
        val xs = TestUtils.build()
        assertEquals(xs.size, 1)
        val x  = xs.head
        assertEquals(x.getImageReferences().size, 0)
        assertEquals(x.getObservations().size, 0)
        assert(x.getAncillaryDatum() == null)
    }

    test("build(2, 2, 2, 2, true)") {
        val xs = TestUtils.build(2, 2, 2, 2, true)
        assertEquals(xs.size, 2)
        val x  = xs.head
        println(x)
        assertEquals(x.getImageReferences().size, 2)
        assertEquals(x.getObservations().size, 2)
        x.getObservations().asScala.foreach(obs => assertEquals(obs.getAssociations().size, 2))
        assert(x.getAncillaryDatum() != null)
    }

    test("create") {
        given df: TestDAOFactory = daoFactory
        val xs                   = TestUtils.create(1, 2, 2, 2, true)
        assertEquals(xs.size, 1)
        val x                    = xs.head
        assert(x.getUuid() != null)
        val dao                  = daoFactory.newImagedMomentDAO()
        val opt                  = Await.result(dao.runTransaction(_.findByUUID(x.getUuid())), timeout)
        assert(opt.isDefined)
        AssertUtils.assertSameImagedMoment(x, opt.get)

    }

    test("create imagedMoment") {
        given df: TestDAOFactory = daoFactory
        val xs                   = TestUtils.create(1)
        assertEquals(xs.size, 1)
        val x                    = xs.head
        assert(x.getUuid() != null)
        val dao                  = daoFactory.newImagedMomentDAO()
        val opt                  = Await.result(dao.runTransaction(_.findByUUID(x.getUuid())), timeout)
        assert(opt.isDefined)
        AssertUtils.assertSameImagedMoment(x, opt.get)

    }

    test("create video reference info") {
        given df: TestDAOFactory = daoFactory
        val vi                   = TestUtils.randomVideoReferenceInfo()
        val dao                  = daoFactory.newCachedVideoReferenceInfoDAO()
        Await.ready(dao.runTransaction(_.create(vi)), timeout)
        assert(vi.getUuid() != null)
        val opt                  = Await.result(dao.runTransaction(_.findByUUID(vi.getUuid())), timeout)
        assert(opt.isDefined)
        val vi2                  = opt.get
        AssertUtils.assertSameVideoReferenceInfo(vi2, vi)

        dao.close()

    }

}

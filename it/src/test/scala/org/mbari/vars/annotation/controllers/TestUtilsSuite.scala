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

package org.mbari.vars.annotation.controllers

import org.mbari.vars.annotation.AssertUtils
import org.mbari.vars.annotation.repository.jpa.{BaseDAOSuite, DerbyTestDAOFactory, TestDAOFactory}

import java.util.concurrent.TimeUnit
import scala.concurrent.{Await, ExecutionContext}

class TestUtilsSuite extends munit.FunSuite {

    implicit val daoFactory: TestDAOFactory = DerbyTestDAOFactory
    implicit val ec: ExecutionContext = ExecutionContext.global
    val timeout = scala.concurrent.duration.Duration(10, TimeUnit.SECONDS)

    test("build") {
        val xs = TestUtils.build()
        assertEquals(xs.size, 1)
        val x = xs.head
        assertEquals(x.imageReferences.size, 0)
        assertEquals(x.observations.size, 0)
        assert(x.ancillaryDatum == null)
    }

    test("build(2, 2, 2, 2, true)") {
         val xs = TestUtils.build(2, 2, 2, 2, true)
        assertEquals(xs.size, 2)
        val x = xs.head
        println(x)
        assertEquals(x.imageReferences.size, 2)
        assertEquals(x.observations.size, 2)
        x.observations.foreach(obs => assertEquals(obs.associations.size, 2))
        assert(x.ancillaryDatum != null)
    }

    test("create") {
        val xs = TestUtils.create(1, 2, 2, 2, true)
        assertEquals(xs.size, 1)
        val x = xs.head
        val dao = daoFactory.newImagedMomentDAO()
        Await.ready(dao.runTransaction(_.create(x)), timeout)
        val opt = Await.result(dao.runTransaction(_.findByUUID(x.uuid)), timeout)
        assert(opt.isDefined)
        AssertUtils.assertSameImagedMoment(x, opt.get)

    }


}

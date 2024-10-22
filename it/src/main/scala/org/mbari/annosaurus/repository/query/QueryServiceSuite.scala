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

package org.mbari.annosaurus.repository.query

import org.mbari.annosaurus.controllers.TestUtils
import org.mbari.annosaurus.repository.jpa.{BaseDAOSuite, JPADAOFactory}
import org.mbari.annosaurus.repository.query.Constraint.In

trait QueryServiceSuite extends BaseDAOSuite {

    given JPADAOFactory = daoFactory

    lazy val queryService = new QueryService(daoFactory.databaseConfig, daoFactory.annotationView)

    test("connection") {
        val d = daoFactory.databaseConfig
        val c = d.newConnection()
        c.close()
    }

    test("query distinct concept") {
        val im = TestUtils.create(5, 2, 1)
        val query = Query(select = Seq("concept"), distinct = true)
        queryService.query(query) match
            case Left(e) => fail(e.getMessage)
            case Right(results) =>
                assertEquals(results.size, 1)
                assertEquals(results.head._2.size, 10)

    }

    test("query distinct concept with limit") {
        val im = TestUtils.create(5, 2, 1)
        val query = Query(select = Seq("concept"), distinct = true, limit = Some(2))
        queryService.query(query) match
            case Left(e) => fail(e.getMessage)
            case Right(results) =>
                assertEquals(results.size, 1)
                assertEquals(results.head._2.size, 2)
    }

    test("query by concept") {
        val im = TestUtils.create(5, 2, 1)
        val query = Query(select = Seq("concept"), distinct = true, where = Seq(In("concept", Seq(im.head.getObservations.iterator().next().getConcept))))
        queryService.query(query) match
            case Left(e) => fail(e.getMessage)
            case Right(results) =>
                assertEquals(results.size, 1)
                assertEquals(results.head._2.size, 1)
    }

}

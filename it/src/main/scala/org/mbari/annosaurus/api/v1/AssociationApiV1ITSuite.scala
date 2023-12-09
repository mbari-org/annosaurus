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

package org.mbari.annosaurus.api.v1

import org.mbari.annosaurus.Constants
import org.mbari.annosaurus.api.WebSuite
import org.mbari.annosaurus.controllers.{AssociationController, BasicDAOFactory, TestUtils}
import org.mbari.vars.annotation.repository.jpa.JPADAOFactory
import org.mbari.vars.annotation.repository.jpa.entity.AssociationEntity

import scala.concurrent.ExecutionContext

trait AssociationApiV1ITSuite extends WebSuite {

    implicit val df: JPADAOFactory = daoFactory
//    implicit val ec: ExecutionContext = ExecutionContext.global
    private val gson = Constants.GSON

    override def beforeAll(): Unit = {
        super.beforeAll()
        addServlet(associationV1Api, "/v1/associations")
        daoFactory.beforeAll()
    }

    override def afterAll(): Unit = {
        daoFactory.afterAll()
        super.afterAll()
    }

    lazy val associationV1Api = {
        val controller = new AssociationController(daoFactory.asInstanceOf[BasicDAOFactory])
        new AssociationV1Api(controller)
    }

    test("POST /bulk") {
        val linkName = "foobarbazbinboobulk"
        val x = TestUtils.create(1, 1, 4).head
        val associations = x.observations.flatMap(_.associations)
        associations.foreach(_.linkName = linkName)
        val json = gson.toJson(associations.toArray)
        println(json)

        put(
            "/v1/associations/bulk",
            body = json,
            headers = Map("Content-Type" -> "application/json", "Accept" -> "application/json")
        ) {
            assertEquals(status, 200)
            val bx = gson.fromJson(body, classOf[Array[AssociationEntity]])
            bx.foreach(b => assertEquals(b.linkName, linkName))
        }

    }
}

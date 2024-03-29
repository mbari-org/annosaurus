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

import scala.jdk.CollectionConverters.*

class SqlServerSuite extends munit.FunSuite:

    val daoFactory = SqlServerTestDAOFactory

    override def beforeAll(): Unit = daoFactory.beforeAll()
    override def afterAll(): Unit  = daoFactory.afterAll()

    test("SqlServer container should be started"):
        assert(daoFactory.container.isRunning())
        val dao = daoFactory.newImagedMomentDAO()
        val all = dao.findAll(Some(0), Some(100))
        assert(all.isEmpty)
        dao.close()

    test("SqlServer init script should have been run"):
        val em = daoFactory.entityManagerFactory.createEntityManager()
        val q  = em.createNativeQuery("SELECT COUNT(*) FROM imaged_moments")
        val r  = q.getResultList().asScala.toList.head.asInstanceOf[Number].longValue()
        assert(r >= 0)

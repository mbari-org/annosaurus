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

import org.hibernate.Session
import org.hibernate.jdbc.Work
import org.mbari.annosaurus.controllers.TestUtils

import java.util.UUID

trait TestDAOFactorySuite extends BaseDAOSuite:

    test("DAOFactory connects to database") {
        val dao = daoFactory.newImagedMomentDAO();
        val i   = exec(dao.runTransaction(d => d.countAll()))
        dao.close()
    }

    test("UUIDs survive round trip to database") {
        val im    = TestUtils.randomImagedMoment()
        val dao   = daoFactory.newImagedMomentDAO()
        exec(dao.runTransaction(d => d.create(im)))
        assert(im.getUuid() != null)
        val uuids = dao.findAllVideoReferenceUUIDs()
        assert(uuids.exists(u => u == im.getVideoReferenceUuid()))
    }

    test("UUIDs do not have endianness issues in database") {
        val im  = TestUtils.randomImagedMoment()
        val dao = daoFactory.newImagedMomentDAO()
        exec(dao.runTransaction(d => d.create(im)))
        assert(im.getUuid() != null)

        // We use straight JDBC to get a UUID from the database as a string
        // This is to avoid any endianness issues that might occur through JPA/Hibernate
        val em = daoFactory.entityManagerFactory.createEntityManager();
        val tx = em.getTransaction()
        tx.begin();

        val session = em.unwrap(classOf[Session]);
        session.doWork(connection =>
            val statement = connection.createStatement()
            val rs        = statement.executeQuery(
                s"select uuid from imaged_moments where uuid = '${im.getUuid()}'"
            )
            rs.next()
            val uuid      = UUID.fromString(rs.getString("uuid"))
//            println(s"uuid: $uuid  ---- ${im.getUuid()}")
            assert(uuid == im.getUuid())
        )

        tx.commit()
        dao.close()

    }

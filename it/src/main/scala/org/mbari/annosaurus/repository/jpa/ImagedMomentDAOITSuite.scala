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

import jakarta.persistence.EntityManager
import java.util.UUID
import org.mbari.vcr4j.time.Timecode
import java.time.Duration
import java.time.Instant
import junit.framework.Test
import org.mbari.annosaurus.controllers.TestUtils



trait ImagedMomentDAOITSuite extends BaseDAOSuite {



    test("create w/ manual transaction") {
        val em = daoFactory.entityManagerFactory.createEntityManager()
        val dao = new ImagedMomentDAOImpl(em)
        val im = dao.newPersistentObject(UUID.randomUUID(), 
            Some(Timecode("01:02:03:04")), 
            Some(Duration.ofSeconds(10)),
            Some(Instant.now))
        val t = em.getTransaction()
        t.begin()
        dao.create(im)
        t.commit()
        dao.close()
        assert(im.getUuid() != null)

    }

    test("create") {
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val im = dao.newPersistentObject(UUID.randomUUID(), 
            Some(Timecode("01:02:03:04")), 
            Some(Duration.ofSeconds(10)),
            Some(Instant.now))
        run(() => dao.create(im))
        // dao.runTransaction(_.create(im))
        // val t = em.getTransaction()
        // t.begin()
        // dao.create(im)
        // t.commit()
        dao.close()
        assert(im.getUuid() != null)

    }

    test("Create 2") {
        val im = TestUtils.build(1, 2, 2, 2, true).head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        run(() => dao.create(im))
        dao.close()
        assert(im.getUuid() != null)
    }

    test("update") {}
    test("delete") {}
    test("deleteByUUID") {}
    test("findByUUID") {}
    test("findAll") {}
    test("findBetweenUpdatedDates") {}
    test("streamBetweenUpdatedDates") {}
    test("streamByVideoReferenceUUIDAndTimestamps") {}
    test("streamVideoReferenceUuidsBetweenUpdateDates") {}
    test("countAll") {}
    test("countWithImages") {}
    test("findWithImages") {}
    test("countByLinkName") {}
    test("findByLinkName") {}
    test("countByConcept") {}
    test("findByConcept") {}
    test("streamByConcept") {}
    test("countByConceptWithImages") {}
    test("countModifiedBeforeDate") {}
    test("findByConceptWithImages") {}
    test("countBetweenUpdatedDates") {}
    test("countAllByVideoReferenceUuids") {}
    test("findAllVideoReferenceUUIDs") {}
    test("findBYVideoReferenceUUID") {}
    test("streamByVideoReferenceUUID") {}
    test("countByVideoReferenceUUID") {}
    test("countByVideoReferenceUUIDWithImages") {}
    test("findWithImageReferences") {}
    test("findByImageReferenceUUID") {}
    test("findByVideoReferenceUUIDAndTimecode") {}
    test("findByVideoReferenceUUIDAndRecordedDate") {}
    test("findByVideoReferenceUUIDAndElapsedTime") {}
    test("findByWindowRequest") {}
    test("findByVideoReferenceUUIDAndIndex") {}
    test("findByObservationUUID") {}
    test("updateRecordedTimestampByObservationUuid") {}
    test("deleteByVideoReferenceUUUID") {}
    test("deleteIfEmpty") {}
    test("deleteIfEmptyByUUID") {}


  
}

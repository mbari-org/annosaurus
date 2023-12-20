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

import org.mbari.annosaurus.repository.jpa.BaseDAOSuite
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import scala.concurrent.ExecutionContext
import org.mbari.annosaurus.AssertUtils
import org.mbari.annosaurus.domain.ImagedMoment
import org.mbari.annosaurus.domain.Annotation


trait AnnotationControllerITSuite extends BaseDAOSuite {
    given JPADAOFactory = daoFactory
    given ExecutionContext = ExecutionContext.global
    lazy val controller = AnnotationController(daoFactory)

    override def beforeAll(): Unit = daoFactory.beforeAll()
    override def afterAll(): Unit  = daoFactory.afterAll()

    test("findByUUID") {
        val im1 = TestUtils.create(1, 2, 3, 2, true).head
        val obs = im1.observations.head
        val opt = exec(controller.findByUUID(obs.uuid))
        opt match
            case None => fail("findByUUID returned None")
            case Some(anno) =>
                val im2 = Annotation.toEntities(Seq(anno)).head
                AssertUtils.assertSameImagedMoment(im1, im2)
        

    }

    test("countByVideoReferenceUUID") {}

    test("findByVideoReferenceUUID") {}

    test("streamByVideoReferenceUUID") {}

    test("streamByVideoReferenceUUIDAndTimestamps") {}

    test("streamByConcurrentRequest") {}

    test("countByConcurrentRequest") {}

    test("streamByMultiRequest") {}

    test("countByMultiRequest") {}

    test("findByImageReferenceUUID") {}

    test("create") {}

    test("bulkCreate") {}

    test("update") {}

    test("bulkUpdate") {}

    test("bulkUpdateRecordedTimestampOnly") {}

    test("delete") {}


}

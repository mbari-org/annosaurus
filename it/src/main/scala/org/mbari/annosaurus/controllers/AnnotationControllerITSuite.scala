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
import scala.jdk.CollectionConverters.*
import org.mbari.annosaurus.etc.jdk.Logging.given


trait AnnotationControllerITSuite extends BaseDAOSuite {
    given JPADAOFactory = daoFactory
    given ExecutionContext = ExecutionContext.global
    lazy val controller = AnnotationController(daoFactory)
    private val log = System.getLogger(getClass.getName)

    override def beforeAll(): Unit = daoFactory.beforeAll()
    override def afterAll(): Unit  = daoFactory.afterAll()

    test("findByUUID") {
        val im1 = TestUtils.create(1, 2, 3, 2, true).head
        log.atInfo.log("im1: " + im1)
        val obs = im1.getObservations.asScala.head
        val opt = exec(controller.findByUUID(obs.getUuid))
        opt match
            case None => fail("findByUUID returned None")
            case Some(anno) =>
                // NOTE: this anno is only 1 observations. THe source imagedMoment has two.
                // this is ok and expected. An annotation maps to a single observation!!
                val im2 = Annotation.toEntities(Seq(anno)).head
                AssertUtils.assertSameImagedMoment(im1, im2, false)
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

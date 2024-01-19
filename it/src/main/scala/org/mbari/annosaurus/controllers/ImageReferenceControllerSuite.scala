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

import org.mbari.annosaurus.domain.ImageReference
import org.mbari.annosaurus.repository.jpa.{BaseDAOSuite, JPADAOFactory}

import scala.jdk.CollectionConverters.*

trait ImageReferenceControllerSuite extends BaseDAOSuite {

    given JPADAOFactory = daoFactory

    override def beforeAll(): Unit = daoFactory.beforeAll()
    override def afterAll(): Unit = daoFactory.afterAll()

    lazy val controller = ImageReferenceController(daoFactory)

    test("create") {
        val im = TestUtils.create().head
        val ir = TestUtils.randomImageReference()
        val ir2 = exec(controller.create(im.getUuid,
            ir.getUrl,
            Some(ir.getDescription),
            Some(ir.getHeight),
            Some(ir.getWidth),
            Some(ir.getFormat)))
        assert(ir2.uuid != null)
        assertEquals(ir.getUrl, ir2.url)
        assertEquals(ir.getDescription, ir2.description.orNull)
        assertEquals(ir.getHeight.intValue(), ir2.heightPixels.orNull)
        assertEquals(ir.getWidth.intValue(), ir2.widthPixels.orNull)
        assertEquals(ir.getFormat, ir2.format.orNull)
    }

    test("update") {
        val im = TestUtils.create(1, nImageReferences = 1).head
        val ir0 = im.getImageReferences.iterator().next()
        val ir1 = TestUtils.randomImageReference()
        val opt = exec(controller.update(ir0.getUuid,
            Some(ir1.getUrl),
            Some(ir1.getDescription),
            Some(ir1.getHeight),
            Some(ir1.getWidth),
            Some(ir1.getFormat)))
        assert(opt.isDefined)
        val ir2 = opt.get
        assertEquals(ir0.getUuid, ir2.uuid.orNull)
        assertEquals(ir1.getUrl, ir2.url)
        assertEquals(ir1.getDescription, ir2.description.orNull)
        assertEquals(ir1.getHeight.intValue(), ir2.heightPixels.orNull)
        assertEquals(ir1.getWidth.intValue(), ir2.widthPixels.orNull)
        assertEquals(ir1.getFormat, ir2.format.orNull)
    }

    test("delete") {
        val im = TestUtils.create(1, 1, nImageReferences = 1).head
        val ir0 = im.getImageReferences.iterator().next()
        val ok = exec(controller.delete(ir0.getUuid))
        assert(ok)
        val opt = exec(controller.findByUUID(ir0.getUuid))
        assert(opt.isEmpty)
    }

    test("findAll") {
        val xs = TestUtils.create(2, nImageReferences = 3)
        val irs0 = xs.flatMap(_.getImageReferences.asScala)
            .toSet
            .map(ir => ImageReference.from(ir))
        val irs1 = exec(controller.findAll()).toSet
        assert(irs1.size >= irs0.size)
        for
            ir <- irs0
        do
            irs1.find(_.uuid == ir.uuid) match
                case None => fail(s"Could not find ${ir.uuid}")
                case Some(ir2) =>
                    assertEquals(ir.url, ir2.url)
                    assertEquals(ir.description, ir2.description)
                    assertEquals(ir.heightPixels, ir2.heightPixels)
                    assertEquals(ir.widthPixels, ir2.widthPixels)
                    assertEquals(ir.format, ir2.format)
    }

    test("findByUUID") {
        val im = TestUtils.create(1, nImageReferences = 1).head
        val ir0 = im.getImageReferences.iterator().next()
        val ir1 = exec(controller.findByUUID(ir0.getUuid))
        assert(ir1.isDefined)
        assertEquals(ir0.getUuid, ir1.get.uuid.orNull)
        assertEquals(ir0.getUrl, ir1.get.url)
        assertEquals(ir0.getDescription, ir1.get.description.orNull)
        assertEquals(ir0.getHeight.intValue(), ir1.get.heightPixels.orNull)
        assertEquals(ir0.getWidth.intValue(), ir1.get.widthPixels.orNull)
        assertEquals(ir0.getFormat, ir1.get.format.orNull)
    }



}

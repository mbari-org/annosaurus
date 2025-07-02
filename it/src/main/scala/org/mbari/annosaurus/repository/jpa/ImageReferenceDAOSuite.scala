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

import org.mbari.annosaurus.AssertUtils
import org.mbari.annosaurus.controllers.TestUtils

import java.net.URI

trait ImageReferenceDAOSuite extends BaseDAOSuite:

    given JPADAOFactory = daoFactory

    test("create") {
        val im                           = TestUtils.create(1).head
        val ir                           = TestUtils.randomImageReference()
        given dao: ImageReferenceDAOImpl = daoFactory.newImageReferenceDAO()
        val imDao                        = daoFactory.newImagedMomentDAO(dao)
        run(() =>
            val im1 = imDao.update(im)
            im1.addImageReference(ir)
            // dao.create(ir)
        )
        run(() => dao.findByUUID(ir.getUuid())) match
            case Some(ir1) => AssertUtils.assertSameImageReference(ir1, ir)
            case None      => fail("Failed to find image reference")
        dao.close()
    }

    test("update") {
        val im                           = TestUtils.create(1, nImageReferences = 1).head
        val ir                           = im.getImageReferences.iterator().next()
        given dao: ImageReferenceDAOImpl = daoFactory.newImageReferenceDAO()
        ir.setUrl(URI.create("http://foo.com").toURL)
        run(() => dao.update(ir))
        run(() => dao.findByUUID(ir.getUuid())) match
            case None        => fail("should have found the entity")
            case Some(value) => AssertUtils.assertSameImageReference(value, ir)
        dao.close()
    }

    test("delete") {
        val im                           = TestUtils.create(1, nImageReferences = 1).head
        val ir                           = im.getImageReferences.iterator().next()
        given dao: ImageReferenceDAOImpl = daoFactory.newImageReferenceDAO()
        run(() =>
            // update brings entity into transactional context
            val ir0 = dao.update(ir)
            ir0.getImagedMoment().removeImageReference(ir0)
            // dao.delete(ir0) // Not needed as removing from imagedmoment in transaction will remove the image reference
        )
        run(() => dao.findByUUID(ir.getUuid())) match
            case Some(_) => fail("should not have found the entity")
            case None    => // good
        dao.close()
    }

    test("deleteByUUID") {
        val im                           = TestUtils.create(1, nImageReferences = 1).head
        val ir                           = im.getImageReferences.iterator().next()
        given dao: ImageReferenceDAOImpl = daoFactory.newImageReferenceDAO()
        run(() => dao.deleteByUUID(ir.getUuid()))
        run(() => dao.findByUUID(ir.getUuid())) match
            case Some(_) => fail("should not have found the entity")
            case None    => // good
        dao.close()
    }

    test("findByUUID") {
        val im                           = TestUtils.create(1, nImageReferences = 1).head
        val ir                           = im.getImageReferences.iterator().next()
        given dao: ImageReferenceDAOImpl = daoFactory.newImageReferenceDAO()
        run(() => dao.findByUUID(ir.getUuid())) match
            case None        => fail("should have found the entity")
            case Some(value) => AssertUtils.assertSameImageReference(value, ir)
        dao.close()
    }

    test("findAll") {
        val im                           = TestUtils.create(1, nImageReferences = 1).head
        val ir                           = im.getImageReferences.iterator().next()
        given dao: ImageReferenceDAOImpl = daoFactory.newImageReferenceDAO()
        val ys                           = run(() => dao.findAll())
        dao.close()
        assert(ys.size >= 1)
        val opt                          = ys.filter(_.getUuid() == ir.getUuid()).headOption
        assert(opt.isDefined)
        AssertUtils.assertSameImageReference(opt.get, ir)
    }

    test("findByURL") {
        val im                           = TestUtils.create(1, nImageReferences = 1).head
        val ir                           = im.getImageReferences.iterator().next()
        given dao: ImageReferenceDAOImpl = daoFactory.newImageReferenceDAO()
        run(() => dao.findByURL(ir.getUrl())) match
            case None        => fail("should have found the entity")
            case Some(value) => AssertUtils.assertSameImageReference(value, ir)
        dao.close()
    }

    test("findByImageName") {
        val im                           = TestUtils.create(1, nImageReferences = 1).head
        val ir                           = im.getImageReferences.iterator().next()
        given dao: ImageReferenceDAOImpl = daoFactory.newImageReferenceDAO()
        val imageName                    = ir.getUrl().getPath().split("/").last
        val xs                           = run(() => dao.findByImageName(imageName))
        assertEquals(xs.size, 1)
        AssertUtils.assertSameImageReference(xs.head, ir)
        dao.close()
    }

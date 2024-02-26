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

import org.mbari.annosaurus.AssertUtils
import org.mbari.annosaurus.domain.{Image, ImageCreateSC}
import org.mbari.annosaurus.repository.jpa.{BaseDAOSuite, JPADAOFactory}
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}

import scala.jdk.CollectionConverters.*

trait ImageControllerSuite extends BaseDAOSuite {

    given JPADAOFactory = daoFactory

    override def beforeAll(): Unit = daoFactory.beforeAll()

    override def afterAll(): Unit = daoFactory.afterAll()

    lazy val controller = new ImageController(daoFactory)

    test("findByUUID") {
        val im       = TestUtils.create(nImageReferences = 1).head
        val i        = im.getImageReferences.iterator().next()
        val opt      = exec(controller.findByUUID(i.getUuid))
        assert(opt.isDefined)
        val expected = Image.from(i, true)
        val obtained = opt.get
        assertEquals(obtained, expected)
    }

    test("findByVideoReferenceUUID") {
        val im = TestUtils.create(nImageReferences = 2).head
        val i  = im.getImageReferences.iterator().next()
        val xs = exec(controller.findByVideoReferenceUUID(im.getVideoReferenceUuid))
        assertEquals(xs.size, im.getImageReferences.size())
    }

    test("findByURL") {
        val im       = TestUtils.create(nImageReferences = 2).head
        val i        = im.getImageReferences.iterator().next()
        val opt      = exec(controller.findByURL(i.getUrl))
        assert(opt.isDefined)
        val expected = Image.from(i, true)
        val obtained = opt.get
        assertEquals(obtained, expected)
    }

    test("findByImageName") {
        val im        = TestUtils.create(nImageReferences = 2).head
        val i         = im.getImageReferences.iterator().next()
        val imageName = i.getUrl.toExternalForm.split("/").last
        val xs        = exec(controller.findByImageName(imageName))
        assertEquals(xs.size, 1)
        val expected  = Image.from(i, true)
        val obtained  = xs.head
        assertEquals(obtained, expected)
    }

    test("bulkCreate") {
        val xs           = TestUtils.build(2, 2, 0, 2)
        val seed         = xs.flatMap(ImageCreateSC.from(_))
        val imageCreates = seed ++ seed // we want to try to insert duplicates
        val images       = exec(controller.bulkCreate(imageCreates))
        assertEquals(images.size, seed.size)

        for (i <- imageCreates) {
            exec(controller.findByURL(i.url)) match {
                case Some(im) =>
                    assertEquals(im.videoReferenceUuid, i.video_reference_uuid)
                    assertEquals(im.url.orNull, i.url)
                    assertEquals(im.timecode, i.timecode)
                    assertEquals(im.elapsedTime.map(_.toMillis), i.elapsed_time_millis)
                    assertEquals(im.recordedTimestamp, i.recorded_timestamp)
                    assertEquals(im.format, i.format)
                    assertEquals(im.widthPixels, i.width_pixels)
                    assertEquals(im.heightPixels, i.height_pixels)
                    assertEquals(im.description, i.description)
                case None     => fail(s"Could not find ImageReference with url=${i.url}")
            }
        }

        // Try to insert duplicates. None should be created
        val images2 = exec(controller.bulkCreate(imageCreates))
        assertEquals(images2.size, 0)
    }

    test("create") {
        val im = TestUtils.create().head
        val i  = TestUtils.randomImageReference()
        val j  = exec(
            controller.create(
                im.getVideoReferenceUuid,
                i.getUrl,
                Option(im.getTimecode),
                Option(im.getElapsedTime),
                Option(im.getRecordedTimestamp),
                Option(i.getFormat),
                Option(i.getWidth),
                Option(i.getHeight),
                Option(i.getDescription)
            )
        )
        assert(j.imageReferenceUuid != null)
        assert(j.imagedMomentUuid != null)
        assertEquals(j.videoReferenceUuid, im.getVideoReferenceUuid)
        assertEquals(j.url.orNull, i.getUrl)
        assertEquals(j.timecode.orNull, Option(im.getTimecode).map(_.toString).orNull)
        assertEquals(j.elapsedTime.orNull, im.getElapsedTime)
        assertEquals(j.recordedTimestamp.orNull, im.getRecordedTimestamp)
        assertEquals(j.format.orNull, i.getFormat)
        assertEquals(j.widthPixels.orNull, i.getWidth.intValue())
        assertEquals(j.heightPixels.orNull, i.getHeight.intValue())
        assertEquals(j.description.orNull, i.getDescription)

    }

    test("update (fields)") {
        val im  = TestUtils.create(nImageReferences = 1).head
        val i   = im.getImageReferences.iterator().next()
        val j   = TestUtils.randomImageReference()
        val opt = exec(
            controller.update(
                i.getUuid,
                Some(im.getVideoReferenceUuid),
                Some(j.getUrl),
                Option(im.getTimecode),
                Option(im.getElapsedTime),
                Option(im.getRecordedTimestamp),
                Option(j.getFormat),
                Option(j.getWidth),
                Option(j.getHeight),
                Option(j.getDescription)
            )
        )
        assert(opt.isDefined)
        val k   = opt.get
        assertEquals(k.videoReferenceUuid, im.getVideoReferenceUuid)
        assertEquals(k.url.orNull, j.getUrl)
        assertEquals(k.timecode.orNull, Option(im.getTimecode).map(_.toString).orNull)
        assertEquals(k.elapsedTime.orNull, im.getElapsedTime)
        assertEquals(k.recordedTimestamp.orNull, im.getRecordedTimestamp)
        assertEquals(k.format.orNull, j.getFormat)
        assertEquals(k.widthPixels.orNull, j.getWidth.intValue())
        assertEquals(k.heightPixels.orNull, j.getHeight.intValue())
        assertEquals(k.description.orNull, j.getDescription)
    }

    test("update (videoReferenceUuid)") {
        val im0 = TestUtils.create(nImageReferences = 1).head
        val im1 = TestUtils.create().head
        val i   = im0.getImageReferences.iterator().next()
        val opt = exec(
            controller.update(
                i.getUuid,
                videoReferenceUUID = Some(im1.getVideoReferenceUuid),
                elapsedTime = Some(im1.getElapsedTime)
            )
        )
        assert(opt.isDefined)
        val k   = opt.get
        assertEquals(k.videoReferenceUuid, im1.getVideoReferenceUuid)
        assertEquals(k.url.orNull, i.getUrl)
        assertEquals(k.elapsedTime.orNull, im1.getElapsedTime)
    }

    test("delete") {
        val im  = TestUtils.create(nImageReferences = 1).head
        val i   = im.getImageReferences.iterator().next()
        val ok  = exec(controller.delete(i.getUuid))
        assert(ok)
        val opt = exec(controller.findByUUID(i.getUuid))
        assert(opt.isEmpty)
    }

}

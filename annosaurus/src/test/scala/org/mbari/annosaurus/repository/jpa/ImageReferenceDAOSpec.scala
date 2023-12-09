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

import org.mbari.vars.annotation.repository.ImageReferenceDAO
import org.mbari.vars.annotation.repository.jpa.entity.{ImageReferenceEntity, ImagedMomentEntity}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.net.URL
import java.time.{Duration, Instant}
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration => SDuration}

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-06-28T17:04:00
  */
class ImageReferenceDAOSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory = TestDAOFactory.Instance

  private[this] val timeout            = SDuration(2, TimeUnit.SECONDS)
  private[this] val imDao              = daoFactory.newImagedMomentDAO()
  private[this] val dao                = daoFactory.newImageReferenceDAO(imDao)
  private[this] val videoReferenceUUID = UUID.randomUUID()
  private[this] val now                = Instant.now()
  private[this] val imagedMoment0 =
    ImagedMomentEntity(Some(videoReferenceUUID), Some(now), elapsedTime = Some(Duration.ofMinutes(1)))
  private[this] val imageReference0 = ImageReferenceEntity(
    new URL("http://www.mbari.org/wp-content/uploads/2015/08/schlining_brian-180.jpg")
  )
  private[this] val imageReference1 = ImageReferenceEntity(
    new URL("https://afleetinglance.files.wordpress.com/2012/07/zazen2.jpg")
  )
  private[this] val newDescription = "A handsome fellow"

  private type IRDAO = ImageReferenceDAO[ImageReferenceEntity]
  def run[R](fn: IRDAO => R): R = Await.result(dao.runTransaction(fn), timeout)

  "ImageReferenceDAOImpl" should "create" in {
    imagedMoment0.addImageReference(imageReference0)
    run(_.create(imageReference0))
    imageReference0.uuid should not be null

    imagedMoment0.addImageReference(imageReference1)
    run(_.create(imageReference0))
  }

  it should "update" in {
    run(d => {
      val ir = d.findByUUID(imageReference0.uuid)
      ir shouldBe defined
      ir.get.description = newDescription
    })

    val ir = run(_.findByUUID(imageReference0.uuid)).head
    ir.description should be(newDescription)

  }

  it should "findAll" in {
    val irs = run(_.findAll()).filter(_.imagedMoment.uuid == imagedMoment0.uuid)
    irs.size should be(2)
  }

  it should "deleteByUUID" in {
    run(_.deleteByUUID(imageReference1.uuid))
    val ir1 = run(_.findByUUID(imageReference1.uuid))
    ir1 shouldBe empty
  }

  it should "delete" in {
    run(_.delete(imageReference0))
    val ir0 = run(_.findByUUID(imageReference0.uuid))
    ir0 shouldBe empty
  }

  override protected def afterAll(): Unit = {
    daoFactory.cleanup()
  }

}

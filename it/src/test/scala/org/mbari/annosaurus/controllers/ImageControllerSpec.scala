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

import org.mbari.annosaurus.domain.*
import org.mbari.annosaurus.repository.jpa.{DerbyTestDAOFactory, TestDAOFactory}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.net.URL
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration as SDuration
import scala.concurrent.{Await, Future}

/**
  * @author Brian Schlining
  * @since 2019-06-04T09:31:00
  */
class ImageControllerSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory         = DerbyTestDAOFactory
  private[this] val controller         = new ImageController(daoFactory)
  private[this] val timeout            = SDuration(200, TimeUnit.SECONDS)
  private[this] val recordedDate       = Instant.now
  private[this] val urlPng             = new URL("http://www.mbari.org/foo.png")
  private[this] val urlJpg             = new URL("http://www.mbari.org/foo.jpg")
  private[this] val videoReferenceUuid = UUID.randomUUID()

  def exec[R](fn: () => Future[R]): R = Await.result(fn.apply(), timeout)

  "ImageController" should "create" in {
    val a = exec(() =>
      controller.create(
        videoReferenceUuid,
        urlPng,
        recordedDate = Some(recordedDate),
        format = Some("image/png"),
        width = Some(1920),
        height = Some(1080),
        description = None
      )
    )

    a.recordedTimestamp should be(recordedDate)
    a.url should be(urlPng)
  }

  it should "find by name" in {
    val a = exec(() => controller.findByImageName("foo"))
    a.size should be(1)
    a.head.url should be(urlPng)

    exec(() =>
      controller.create(
        videoReferenceUuid,
        urlJpg,
        recordedDate = Some(recordedDate),
        format = Some("image/jpg"),
        width = Some(1920),
        height = Some(1080),
        description = None
      )
    )

    val b = exec(() => controller.findByImageName("foo"))
    b.size should be(2)
//    println(b)
    b.head.url should not be b.last.url

    val c = exec(() => controller.findByImageName("foo.png"))
    c.size should be(1)
    c.head.url should be(urlPng)
//    println(c)

    val d = exec(() => controller.findByImageName("foo.jpg"))
    d.size should be(1)
    d.head.url should be(urlJpg)

  }

  it should "create using image_reference_uuid" in {
    val uuid = UUID.randomUUID()
    val url  = new URL("http://www.mbari.org/foo_im.png")
    val a = exec(() =>
      controller.create(
        videoReferenceUuid,
        url,
        recordedDate = Some(recordedDate),
        format = Some("image/png"),
        width = Some(1920),
        height = Some(1080),
        description = None,
        imageReferenceUUID = Some(uuid)
      )
    )
    a.imageReferenceUuid should be(uuid)

    val c = exec(() => controller.findByImageName("foo_im.png"))
    c.size should be(1)
    val i = c.head
    i.url should be(url)
    i.imageReferenceUuid should be(uuid)

  }

  override protected def afterAll(): Unit = {
    daoFactory.cleanup()
  }

}

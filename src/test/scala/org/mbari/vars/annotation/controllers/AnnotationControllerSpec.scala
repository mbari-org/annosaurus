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

package org.mbari.vars.annotation.controllers

import java.time.{ Duration, Instant }
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.dao.jpa.TestDAOFactory
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers }
import org.slf4j.LoggerFactory

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.{ Duration => SDuration }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author Brian Schlining
 * @since 2017-01-23T15:12:00
 */
class AnnotationControllerSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory = TestDAOFactory.Instance
  private[this] val controller = new AnnotationController(daoFactory.asInstanceOf[BasicDAOFactory])
  private[this] val timeout = SDuration(200, TimeUnit.SECONDS)
  private[this] val recordedDate = Instant.now()
  private[this] val log = LoggerFactory.getLogger(getClass)

  def exec[R](fn: () => Future[R]): R = Await.result(fn.apply(), timeout)

  "AnnotationController" should "create by recorded timestamp" in {
    val a = exec(() => controller.create(
      UUID.randomUUID(),
      "Nanomia bijuga",
      "brian",
      recordedDate = Some(recordedDate)))
    a.concept should be("Nanomia bijuga")
    a.observer should be("brian")
    a.recordedTimestamp should be(recordedDate)
  }

  it should "create using existing recorded timestamp" in {
    val a = exec(() => controller.create(
      UUID.randomUUID(),
      "Grimpoteuthis",
      "brian",
      recordedDate = Some(recordedDate)))
    a.concept should be("Grimpoteuthis")
    a.observer should be("brian")
    a.recordedTimestamp should be(recordedDate)
  }

  it should "find by videoReferenceUuid" in {
    val a = exec(() => controller.create(
      UUID.randomUUID(),
      concept = "Slime mold",
      observer = "brian",
      recordedDate = Some(recordedDate)))

    val b = exec(() => controller.findByVideoReferenceUUID(a.videoReferenceUuid))
    b should not be empty
    b.size should be(1)
    b.head.concept should be("Slime mold")
  }

  it should "create and update" in {
    val a = exec(() => controller.create(
      UUID.randomUUID(),
      "Grimpoteuthis",
      "brian",
      recordedDate = Some(recordedDate)))
    a.concept should be("Grimpoteuthis")
    a.observer should be("brian")
    a.recordedTimestamp should be(recordedDate)

    val b = exec(() => controller.update(a.observationUuid, Some(a.videoReferenceUuid),
      concept = Some("Nanomia bijuga")))
    b should not be (empty)
    b.get.concept should be("Nanomia bijuga")
  }

  it should "create and update with different elapsed times" in {
    val et0 = Duration.ofSeconds(30)
    val et1 = Duration.ofSeconds(60)
    val a = exec(() => controller.create(
      UUID.randomUUID(),
      "Grimpoteuthis",
      "brian",
      elapsedTime = Some(et0)))
    a.concept should be("Grimpoteuthis")
    a.observer should be("brian")
    a.elapsedTime should be(et0)

    val b = exec(() => controller.update(
      a.observationUuid,
      Some(a.videoReferenceUuid),
      concept = Some("Nanomia bijuga"),
      elapsedTime = Some(et1)))
    b should not be (empty)
    b.get.concept should be("Nanomia bijuga")
    b.get.elapsedTime should be(et1)

    val c = exec(() => controller.update(
      a.observationUuid,
      elapsedTime = Some(et0)))
    c should not be (empty)
    c.get.elapsedTime should be(et0)
  }

//  it should "report insert benchmark" in {
//    val start = System.nanoTime()
//    val n = 10000
//    for (i <- 0 until n) {
//      exec(() => controller.create(
//        UUID.randomUUID(),
//        "Nanomia bijuga",
//        "brian",
//        recordedDate = Some(recordedDate)))
//    }
//    val end = System.nanoTime();
//    val nanos = end - start
//    val duration = Duration.ofNanos(nanos)
//    log.info(s"Inserted $n records in $duration")
//  }

  protected override def afterAll(): Unit = {
    daoFactory.cleanup()
  }

}

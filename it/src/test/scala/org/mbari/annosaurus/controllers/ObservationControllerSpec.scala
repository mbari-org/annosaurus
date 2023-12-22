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
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.time.{Duration, Instant}
import java.util as ju
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration as SDuration
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters.*

class ObservationControllerSpec extends AnyFunSpec with Matchers {

  private[this] val daoFactory = DerbyTestDAOFactory
  private[this] val annoController = new AnnotationController(
    daoFactory
  )

  private[this] val controller = new ObservationController(
    daoFactory
  )
  private[this] val timeout = SDuration(200, TimeUnit.SECONDS)

  def exec[R](fn: () => Future[R]): R = Await.result(fn.apply(), timeout)

  describe("ObservationController") {
    describe("deleteDuration") {
      it("should delete duration") {

        // create an annotation
        val recordedDate = Instant.now()
        val duration     = Duration.ofSeconds(5)
        val a = exec(() =>
          annoController
            .create(
              ju.UUID.randomUUID(),
              "Nanomia bijuga",
              "brian",
              recordedDate = Some(recordedDate),
              duration = Some(Duration.ofSeconds(5))
            )
        )
        a.concept should be("Nanomia bijuga")
        a.observer should be("brian")
        a.recordedTimestamp should be(recordedDate)
        a.duration should be(duration)

        // look up and verify no duration
        val opt0 = exec(() => controller.findByUUID(a.observationUuid.get))
        opt0 should not be None
        val obs0 = opt0.get
        obs0.getDuration() should be(duration)

        // delete duration
        val opt = exec(() => controller.deleteDuration(a.observationUuid.get))
        opt should not be None
        val obs = opt.get
        obs.getDuration() should be(null)
        obs.getImagedMoment().getRecordedDate().toEpochMilli should be(recordedDate.toEpochMilli())

        // look up and verify no duration
        val opt1 = exec(() => controller.findByUUID(obs.getUuid()))
        opt1 should not be None
        val obs1 = opt1.get
        obs1.getDuration() should be(null)

      }
    }
  }

}

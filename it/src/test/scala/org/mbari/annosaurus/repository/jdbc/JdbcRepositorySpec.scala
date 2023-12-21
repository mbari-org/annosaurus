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

package org.mbari.annosaurus.repository.jdbc

import org.mbari.annosaurus.Constants
import org.mbari.annosaurus.controllers.{AnnotationController, TestEntityFactory}
import org.mbari.annosaurus.domain.*
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.repository.jpa.{DerbyTestDAOFactory, JPADAOFactory, TestDAOFactory}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration as SDuration
import scala.concurrent.{Await, Future}
import scala.io.Source

/**
  * @author Brian Schlining
  * @since 2019-10-22T15:02:00
  */
class JdbcRepositorySpec extends AnyFunSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory    = DerbyTestDAOFactory
  private[this] val controller    = new AnnotationController(daoFactory)
  private[this] val entityFactory = new TestEntityFactory(daoFactory)
  // HACK Assumes where using JDADAPFactory!
  private[this] val repository: JdbcRepository = {
    val entityManagerFactory = daoFactory.asInstanceOf[JPADAOFactory].entityManagerFactory
    new JdbcRepository(entityManagerFactory)
  }
  private[this] val timeout = SDuration(200, TimeUnit.SECONDS)

  def exec[R](fn: () => Future[R]): R = Await.result(fn.apply(), timeout)

  override protected def afterAll(): Unit = {
    daoFactory.cleanup()
  }

  private def loadAnnos(): Seq[Annotation] = {
    val url    = getClass.getResource("/json/annotation_full_dive.json").toURI
    val source = Source.fromFile(url, "UTF-8")
    val json = source
      .getLines()
      .mkString("\n")
    source.close()

    // Insert all annotations
    val annos = json.reify[Array[Annotation]].getOrElse(throw new RuntimeException("Failed to parse"))
    annos should not be empty
    annos.isEmpty should be(false)
    val newAnnos = exec(() => controller.bulkCreate(annos))
    newAnnos.size should be(annos.size)
    newAnnos
  }

  describe("JdbcRepository") {
    describe("delete") {
      it("should deleteByVideoReferenceUuid") {
        val newAnnos = loadAnnos()
        // Delete them
        val videoReferenceUuid = newAnnos.head.videoReferenceUuid.get
        val deleteCount        = repository.deleteByVideoReferenceUuid(videoReferenceUuid)
        println(deleteCount.stringify)

        // Verify that they are gone
        val foundAnnos = repository.findByVideoReferenceUuid(videoReferenceUuid)
        foundAnnos should be(empty)
      }
    }

    describe("find") {

      it("should findImagedMomentUuidsByConceptWithImages") {
        val newAnnos           = loadAnnos()
        val videoReferenceUuid = newAnnos.head.videoReferenceUuid.get

        val n = repository.findImagedMomentUuidsByConceptWithImages("Myxoderma platyacanthum")
        n.size should be(2)

        val deleteCount = repository.deleteByVideoReferenceUuid(videoReferenceUuid)
        deleteCount.observationCount should be(newAnnos.size)

      }

      it("should findImagesByVideoReferenceUuid") {
        val newAnnos           = loadAnnos()
        val videoReferenceUuid = newAnnos.head.videoReferenceUuid.get

        val xs = repository.findImagesByVideoReferenceUuid(videoReferenceUuid)
        xs.size should be(62)

        val deleteCount = repository.deleteByVideoReferenceUuid(videoReferenceUuid)
        deleteCount.observationCount should be(newAnnos.size)
      }

    }
  }

  describe("count") {
    it("should countImagesByVideoReferenceUuid") {
        val newAnnos           = loadAnnos()
        val videoReferenceUuid = newAnnos.head.videoReferenceUuid.get
        val xs = repository.countImagesByVideoReferenceUuid(videoReferenceUuid)
        xs should be (62)
        val deleteCount = repository.deleteByVideoReferenceUuid(videoReferenceUuid)
        deleteCount.observationCount should be(newAnnos.size)
      }
  }

  override protected def beforeAll(): Unit = {
    daoFactory.cleanup()
  }
}

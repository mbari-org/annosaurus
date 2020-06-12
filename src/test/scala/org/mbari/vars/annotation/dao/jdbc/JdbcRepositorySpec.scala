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

package org.mbari.vars.annotation.dao.jdbc

import java.time.{Duration, Instant}
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.Constants
import org.mbari.vars.annotation.controllers.{
  AnnotationController,
  BasicDAOFactory,
  TestEntityFactory
}
import org.mbari.vars.annotation.dao.jpa.{AnnotationImpl, JPADAOFactory, TestDAOFactory}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.{Duration => SDuration}
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.util.Random

/**
  * @author Brian Schlining
  * @since 2019-10-22T15:02:00
  */
class JdbcRepositorySpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory    = TestDAOFactory.Instance
  private[this] val controller    = new AnnotationController(daoFactory.asInstanceOf[BasicDAOFactory])
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

  "JdbcRepository" should "delete by videoReferenceUuid" in {
    // --- Create some annotations
    // Read data
    val url    = getClass.getResource("/json/annotation_full_dive.json").toURI
    val source = Source.fromFile(url, "UTF-8")
    val json = source
      .getLines()
      .mkString("\n")
    source.close()

    // Insert all annotations
    val annos = Constants.GSON.fromJson(json, classOf[Array[AnnotationImpl]])
    annos should not be null
    annos.isEmpty should be(false)
    val newAnnos = exec(() => controller.bulkCreate(annos))
    newAnnos.size should be(annos.size)

    // Delete them
    val videoReferenceUuid = newAnnos.head.videoReferenceUuid
    val deleteCount        = repository.deleteByVideoReferenceUuid(videoReferenceUuid)
    println(Constants.GSON.toJson(deleteCount))

    // Verify that they are gone
    val foundAnnos = repository.findByVideoReferenceUuid(videoReferenceUuid)
    foundAnnos should be(empty)
  }

  override protected def beforeAll(): Unit = {
    daoFactory.cleanup()
  }
}

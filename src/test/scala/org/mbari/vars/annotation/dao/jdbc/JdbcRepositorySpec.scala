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

import org.mbari.vars.annotation.controllers.{AnnotationController, BasicDAOFactory, TestEntityFactory}
import org.mbari.vars.annotation.dao.jpa.{AnnotationImpl, JPADAOFactory, TestDAOFactory}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.duration.{Duration => SDuration}
import scala.concurrent.{Await, Future}
import scala.util.Random

/**
 * @author Brian Schlining
 * @since 2019-10-22T15:02:00
 */
class JdbcRepositorySpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory = TestDAOFactory.Instance
  private[this] val controller = new AnnotationController(daoFactory.asInstanceOf[BasicDAOFactory])
  private[this] val entityFactory = new TestEntityFactory(daoFactory)
  // HACK Assumes where using JDADAPFactory!
  private[this] val repository: JdbcRepository = {
    val entityManagerFactory = daoFactory.asInstanceOf[JPADAOFactory].entityManagerFactory
    new JdbcRepository(entityManagerFactory)
  }
  private[this] val timeout = SDuration(200, TimeUnit.SECONDS)

  def exec[R](fn: () => Future[R]): R = Await.result(fn.apply(), timeout)

  protected override def afterAll(): Unit = {
    daoFactory.cleanup()
  }

  override protected def beforeAll(): Unit = {
    daoFactory.cleanup()
  }
}

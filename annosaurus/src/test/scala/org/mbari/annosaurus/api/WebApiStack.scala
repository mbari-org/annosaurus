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

package org.mbari.annosaurus.api

import org.mbari.annosaurus.Constants
import org.mbari.annosaurus.controllers.BasicDAOFactory
import org.mbari.annosaurus.repository.jpa.TestDAOFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatra.test.scalatest.ScalatraFlatSpec

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

/** @author
  *   Brian Schlining
  * @since 2016-09-08T10:38:00
  */
trait WebApiStack extends ScalatraFlatSpec with BeforeAndAfterAll {

    protected val gson                              = Constants.GSON
    protected val daoFactory                        = TestDAOFactory.Instance.asInstanceOf[BasicDAOFactory]
    implicit val executionContext: ExecutionContext = ExecutionContext.global

    override protected def afterAll(): Unit = {
        val dao = daoFactory.newImagedMomentDAO()

        val f = dao.runTransaction(d => {
            val all = d.findAll()
            all.foreach(d.delete)
        })
        f.onComplete(_ => dao.close())
        Await.result(f, Duration(4, TimeUnit.SECONDS))

        super.afterAll()
    }

}

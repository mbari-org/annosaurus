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

import org.mbari.annosaurus.repository.DAO
import java.util.concurrent.TimeUnit
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.{Duration => SDuration}

trait BaseDAOSuite extends munit.FunSuite {

    implicit val ec: ExecutionContext = ExecutionContext.global

    def daoFactory: TestDAOFactory
    private val timeout = SDuration(2, TimeUnit.SECONDS)

    def exec[T](future: Future[T]): T = Await.result(future, timeout)

    def run[T](thunk: () => T)(implicit dao: DAO[_]): T =
        exec(dao.runTransaction(_ => thunk.apply()))

    override def afterEach(context: AfterEach): Unit =
        super.afterEach(context)
    daoFactory.cleanup()
}

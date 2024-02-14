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

import org.mbari.annosaurus.PersistentObject
import org.mbari.annosaurus.repository.DAO
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import org.mbari.annosaurus.repository.jpa.entity.IPersistentObject

/** @author
  *   Brian Schlining
  * @since 2016-06-25T17:17:00
  */
trait BaseController[A <: IPersistentObject, B <: DAO[A], C] {

    def daoFactory: JPADAOFactory

    def newDAO(): B

    def transform(a: A): C

    protected def exec[T](fn: B => T)(implicit ec: ExecutionContext): Future[T] = {
        val dao = newDAO()
        val f   = dao.runTransaction(fn)
        f.onComplete(_ => dao.close())
        f
    }

    def delete(uuid: UUID)(implicit ec: ExecutionContext): Future[Boolean] = {
        def fn(dao: B): Boolean = {
            dao.findByUUID(uuid) match {
                case Some(v) =>
                    dao.delete(v)
                    true
                case None    =>
                    false
            }
        }
        exec(fn)
    }

    def findAll(limit: Option[Int] = None, offset: Option[Int] = None)(implicit
        ec: ExecutionContext
    ): Future[Iterable[C]] =
        exec(d => d.findAll(limit, offset).map(transform))

    def findByUUID(uuid: UUID)(implicit ec: ExecutionContext): Future[Option[C]] =
        exec(d => d.findByUUID(uuid).map(transform))

}

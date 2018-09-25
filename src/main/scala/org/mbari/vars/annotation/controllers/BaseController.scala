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

import java.util.UUID

import org.mbari.vars.annotation.PersistentObject
import org.mbari.vars.annotation.dao.DAO

import scala.concurrent.{ ExecutionContext, Future }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-25T17:17:00
 */
trait BaseController[A <: PersistentObject, B <: DAO[A]] {

  def daoFactory: BasicDAOFactory

  def newDAO(): B

  protected def exec[T](fn: B => T)(implicit ec: ExecutionContext): Future[T] = {
    val dao = newDAO()
    val f = dao.runTransaction(fn)
    f.onComplete(t => dao.close())
    f
  }

  def delete(uuid: UUID)(implicit ec: ExecutionContext): Future[Boolean] = {
    def fn(dao: B): Boolean = {
      dao.findByUUID(uuid) match {
        case Some(v) =>
          dao.delete(v)
          true
        case None =>
          false
      }
    }
    exec(fn)
  }

  def findAll()(implicit ec: ExecutionContext): Future[Iterable[A]] =
    exec(d => d.findAll())

  def findByUUID(uuid: UUID)(implicit ec: ExecutionContext): Future[Option[A]] =
    exec(d => d.findByUUID(uuid))

}

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

package org.mbari.vars.annotation.dao

import java.util.UUID

import org.mbari.vars.annotation.PersistentObject

import scala.concurrent.{ExecutionContext, Future}

/**
  * All DAOs should implement this trait as it defines the minimum CRUD methods needed.
  *
  * @author Brian Schlining
  * @since 2016-05-05T12:44:00
  * @tparam A The type of the entity
  */
trait DAO[A <: PersistentObject] {

  def newPersistentObject(): A
  def create(entity: A): Unit
  def update(entity: A): A
  def delete(entity: A): Unit
  def deleteByUUID(primaryKey: UUID): Unit
  def findByUUID(primaryKey: UUID): Option[A]
  def findAll(limit: Option[Int] = None, offset: Option[Int] = None): Iterable[A]
  def runTransaction[R](fn: this.type => R)(implicit ec: ExecutionContext): Future[R]
  def close(): Unit

}

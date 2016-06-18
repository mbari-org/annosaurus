package org.mbari.vars.annotation.dao

import java.util.UUID

import org.mbari.vars.annotation.PersistentObject

import scala.concurrent.{ ExecutionContext, Future }

/**
 * All DAOs should implement this trait as it defines the minimum CRUD methods needed.
 *
 * @author Brian Schlining
 * @since 2016-05-05T12:44:00
 * @tparam A The type of the entity
 */
trait DAO[A <: PersistentObject] {

  def create(entity: A): Unit
  def update(entity: A): A
  def delete(entity: A): Unit
  def deleteByUUID(primaryKey: UUID): Unit
  def findByUUID(primaryKey: UUID): Option[A]
  def findAll(): Iterable[A]
  def runTransaction[R](fn: this.type => R)(implicit ec: ExecutionContext): Future[R]
  def close(): Unit

}

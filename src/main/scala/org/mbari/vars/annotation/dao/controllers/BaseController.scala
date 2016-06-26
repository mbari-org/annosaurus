package org.mbari.vars.annotation.dao.controllers

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

  def findAll(implicit ec: ExecutionContext): Future[Iterable[A]] =
    exec(d => d.findAll())

  def findByUUID(uuid: UUID)(implicit ec: ExecutionContext): Future[Option[A]] =
    exec(d => d.findByUUID(uuid))

}

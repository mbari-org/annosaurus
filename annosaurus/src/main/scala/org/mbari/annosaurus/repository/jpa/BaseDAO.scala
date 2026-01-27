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

import jakarta.persistence.{EntityManager, Query}
import org.hibernate.jpa.QueryHints
import org.mbari.annosaurus.etc.jdk.Loggers.given
import org.mbari.annosaurus.repository.DAO
import org.mbari.annosaurus.repository.jpa.entity.IPersistentObject
import org.mbari.annosaurus.repository.jpa.entity.extensions.*
import org.mbari.annosaurus.repository.jpa.extensions.*

import java.lang.System.Logger.Level
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*
import scala.reflect.{classTag, ClassTag}

/**
 * @author
 *   Brian Schlining
 * @since 2016-05-06T11:18:00
 */
abstract class BaseDAO[B <: IPersistentObject: ClassTag](val entityManager: EntityManager) extends DAO[B]:

    private val log = System.getLogger(getClass.getName)

    if log.isLoggable(Level.DEBUG) then
        val props = entityManager.getProperties
        if props.containsKey(BaseDAO.JDBC_URL_KEY) then
            log.atDebug
                .log(
                    s"Wrapping EntityManager with DAO for database: ${props.get(BaseDAO.JDBC_URL_KEY)}"
                )

    def find(obj: B): Option[B] =
        obj.primaryKey.flatMap(pk => Option(entityManager.find(obj.getClass, pk)))

    def findByNamedQuery(
        name: String,
        namedParameters: Map[String, Any] = Map.empty,
        limit: Option[Int] = None,
        offset: Option[Int] = None,
        readOnly: Boolean = false
    ): List[B] =
        findByTypedNamedQuery[B](name, namedParameters, limit, offset, readOnly)

    def findByTypedNamedQuery[C](
        name: String,
        namedParameters: Map[String, Any] = Map.empty,
        limit: Option[Int] = None,
        offset: Option[Int] = None,
        readOnly: Boolean = false
    ): List[C] =
        if log.isLoggable(Level.DEBUG) then log.atDebug.log(s"JPA Query => $name using $namedParameters")
        val query = entityManager.createNamedQuery(name)
        if readOnly then query.setHint(QueryHints.HINT_READONLY, true)
        limit.foreach(query.setMaxResults)
        offset.foreach(query.setFirstResult)
        namedParameters.foreach { case (a, b) => query.setParameter(a, b) }
        query
            .getResultList
            .asScala
            .toList
            .map(_.asInstanceOf[C])

    /**
     * Fetches the results as a stream. This better allows for fetching large sets and returning them as chunked
     * responses.
     * @param name
     * @param namedParameters
     * @param limit
     * @param offset
     * @return
     */
    def streamByNamedQuery(
        name: String,
        namedParameters: Map[String, Any] = Map.empty,
        limit: Option[Int] = None,
        offset: Option[Int] = None,
        readOnly: Boolean = false
    ): java.util.stream.Stream[B] =

        val query = entityManager.createNamedQuery(name)
        if readOnly then query.setHint(QueryHints.HINT_READONLY, true)
        limit.foreach(query.setMaxResults)
        offset.foreach(query.setFirstResult)
        namedParameters.foreach { case (a, b) => query.setParameter(a, b) }
        query
            .getResultStream
            .map(b => b.asInstanceOf[B])

    def executeNamedQuery(name: String, namedParameters: Map[String, Any] = Map.empty): Int =
        val query = entityManager.createNamedQuery(name)
        namedParameters.foreach { case (a, b) => query.setParameter(a, b) }
        query.executeUpdate()

    /**
     * Lookup entity by primary key. A DAO will only return entities of their type. Also, note that I had to use a
     * little scala reflection magic here
     *
     * @param primaryKey
     * @return
     */
    override def findByUUID(primaryKey: UUID): Option[B] =
        Option(entityManager.find(classTag[B].runtimeClass, primaryKey).asInstanceOf[B])

    override def deleteByUUID(primaryKey: UUID): Unit =
        findByUUID(primaryKey).foreach(delete)

    override def runTransaction[R](fn: this.type => R)(implicit ec: ExecutionContext): Future[R] =

        def fn2(em: EntityManager): R = fn.apply(this)
        entityManager.runTransaction(fn2)

    override def runReadOnlyTransaction[R](fn: this.type => R)(implicit ec: ExecutionContext): Future[R] =

        def fn2(em: EntityManager): R = fn.apply(this)
        entityManager.runReadOnlyTransaction(fn2)

    override def create(entity: B): Unit = entityManager.persist(entity)

    override def update(entity: B): B = entityManager.merge(entity)

    override def delete(entity: B): Unit = entityManager.remove(entity)

    def close(): Unit = if entityManager.isOpen then entityManager.close()

    override def flush(): Unit = if entityManager.isOpen then entityManager.flush()

    override def commit(): Unit = if entityManager.isOpen then entityManager.getTransaction.commit()

    override def isDetached(entity: B): Boolean =
        entity.primaryKey.isEmpty              // must not be transient
            && !entityManager.contains(entity) // must not be managed
            && find(entity).isDefined          // must not have been removed

    def setUuidParameter(query: Query, position: Int, uuid: UUID): Query =
        // if (DatabaseProductName.isPostgreSQL()) {
        //     query.setParameter(position, uuid)
        // }
        // else {
        query.setParameter(position, uuid.toString.toLowerCase())
        // }

object BaseDAO:
    val JDBC_URL_KEY = "jakarta.persistence.jdbc.url"

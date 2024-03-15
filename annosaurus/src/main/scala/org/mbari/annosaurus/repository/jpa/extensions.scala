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

import jakarta.persistence.EntityManager

import scala.concurrent.{ExecutionContext, Future}
import org.mbari.annosaurus.etc.jdk.Logging.given
import scala.util.control.NonFatal

/** Implicits used in this package
  *
  * @author
  *   Brian Schlining
  * @since 2016-05-06T13:34:00
  */
object extensions:

    private val log = System.getLogger(getClass.getName)

    extension (entityManager: EntityManager)
        def runTransaction[R](fn: EntityManager => R)(implicit ec: ExecutionContext): Future[R] =
            Future:
                runTransactionSync(fn)

        def runTransactionSync[R](fn: EntityManager => R): R =
            val transaction = entityManager.getTransaction
            transaction.begin()
            try
                val n = fn.apply(entityManager)
                transaction.commit()
                n
            catch
                case NonFatal(e) =>
                    log.atError.withCause(e).log("Error running transaction")
                    throw e
            finally if transaction.isActive then transaction.rollback()

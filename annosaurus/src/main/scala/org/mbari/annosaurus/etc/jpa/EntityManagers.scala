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

package org.mbari.annosaurus.etc.jpa

import jakarta.persistence.EntityManager
import org.mbari.annosaurus.etc.jdk.Loggers.given

import scala.util.control.NonFatal

object EntityManagers:

    private val log = System.getLogger(getClass.getName)

    extension (entityManager: EntityManager)
        def runTransaction[R](fn: EntityManager => R): Either[Throwable, R] =

            val transaction = entityManager.getTransaction
            transaction.begin()
            try
                val n = fn.apply(entityManager)
                transaction.commit()
                Right(n)
            catch
                case NonFatal(e) =>
                    log.atError.withCause(e).log("Error in transaction: " + e.getCause)
                    Left(e)
            finally if transaction.isActive then transaction.rollback()

        // def runQuery[R](fn: EntityManager => R): Either[Throwable, R] =
        //     entityManager.unwrap(classOf[Session]).setDefaultReadOnly(true)
        //     val transaction = entityManager.getTransaction

        //     try
        //         transaction.begin()
        //         val n = fn.apply(entityManager)
        //         // transaction.commit()
        //         Right(n)
        //     catch
        //         case NonFatal(e) =>
        //             log.atError.withCause(e).log("Error in transaction: " + e.getCause)
        //             Left(e)
        //     finally
        //         if transaction.isActive then
        //             transaction.rollback()

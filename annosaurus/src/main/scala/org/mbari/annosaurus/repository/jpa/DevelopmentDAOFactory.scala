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

import com.typesafe.config.ConfigFactory
import jakarta.persistence.EntityManagerFactory

import scala.util.Try

/**
 * DAOFactory for creating development database DAOs
 *
 * @author
 *   Brian Schlining
 * @since 2016-05-23T15:57:00
 */
object DevelopmentDAOFactory extends JPADAOFactory:

    private val config           = ConfigFactory.load()
    private val productName      =
        Try(config.getString("org.mbari.vars.annotation.database.development.name"))
            .getOrElse("Auto")
    private val developmentProps = Map(
        "eclipselink.connection-pool.default.initial"           -> "2",
        "eclipselink.connection-pool.default.max"               -> "16",
        "eclipselink.connection-pool.default.min"               -> "2",
        "eclipselink.logging.level"                             -> "FINE",
        "eclipselink.logging.session"                           -> "false",
        "eclipselink.logging.thread"                            -> "false",
        "eclipselink.logging.timestamp"                         -> "false",
        "eclipselink.target-database"                           -> productName,
        "jakarta.persistence.database-product-name"             -> productName,
        "jakarta.persistence.schema-generation.database.action" -> "create"
    )

    lazy val entityManagerFactory: EntityManagerFactory =
        val driver   = config.getString("org.mbari.vars.annotation.database.development.driver")
        val url      = config.getString("org.mbari.vars.annotation.database.development.url")
        val user     = config.getString("org.mbari.vars.annotation.database.development.user")
        val password = config.getString("org.mbari.vars.annotation.database.development.password")
        EntityManagerFactories(url, user, password, driver, developmentProps)

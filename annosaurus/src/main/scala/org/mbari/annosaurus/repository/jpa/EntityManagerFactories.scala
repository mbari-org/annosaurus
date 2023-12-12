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
import jakarta.persistence.{EntityManagerFactory, Persistence}
import org.eclipse.persistence.config.PersistenceUnitProperties
import org.slf4j.LoggerFactory
import scala.jdk.CollectionConverters._

/** https://stackoverflow.com/questions/4106078/dynamic-jpa-connection
  *
  * THis factory allows us to instantiate an javax.persistence.EntityManager from the basic
  * parameters (url, driver, password, username). You can pass in a map of additional properties to
  * customize the EntityManager.
  *
  * @author
  *   Brian Schlining
  * @since 2016-05-05T17:29:00
  */
object EntityManagerFactories {

    private[this] val log = LoggerFactory.getLogger(getClass)

    private lazy val config = ConfigFactory.load()

    // <property name="eclipselink.weaving" value="static"/>
    val PRODUCTION_PROPS = Map(
        "eclipselink.connection-pool.default.initial"           -> "2",
        "eclipselink.connection-pool.default.max"               -> "16",
        "eclipselink.connection-pool.default.min"               -> "2",
        "eclipselink.logging.logger"                            -> "org.eclipse.persistence.logging.slf4j.SLF4JLogger",
        "eclipselink.logging.session"                           -> "false",
        "eclipselink.logging.thread"                            -> "false",
        "eclipselink.logging.timestamp"                         -> "false",
        "eclipselink.weaving"                                   -> "static",
        "jakarta.persistence.schema-generation.database.action" -> "none", // create,none
        "jakarta.persistence.sharedCache.mode"                  -> "ENABLE_SELECTIVE",
        PersistenceUnitProperties.SESSION_CUSTOMIZER            -> "org.mbari.annosaurus.repository.jpa.UUIDSequence"
    )

    def apply(properties: Map[String, String]): EntityManagerFactory = {
        val props = PRODUCTION_PROPS ++ properties
        val emf   = Persistence.createEntityManagerFactory("annosaurus", props.asJava)
        if (log.isInfoEnabled()) {
            val props = emf
                .getProperties
                .asScala
                .map(a => s"${a._1} : ${a._2}")
                .toList
                .sorted
                .mkString("\n")
            log.info(s"EntityManager Properties:\n${props}")
        }
        emf
    }

    def apply(
        url: String,
        username: String,
        password: String,
        driverName: String,
        properties: Map[String, String] = Map.empty
    ): EntityManagerFactory = {

        val map = Map(
            "jakarta.persistence.jdbc.url"      -> url,
            "jakarta.persistence.jdbc.user"     -> username,
            "jakarta.persistence.jdbc.password" -> password,
            "jakarta.persistence.jdbc.driver"   -> driverName
        )
        apply(map ++ properties)
    }

    def apply(configNode: String): EntityManagerFactory = {
        val driver      = config.getString(configNode + ".driver")
        val logLevel    = config.getString("database.loglevel")
        val password    = config.getString(configNode + ".password")
        val productName = config.getString(configNode + ".name")
        val url         = config.getString(configNode + ".url")
        val user        = config.getString(configNode + ".user")
        val props       = Map(
            "eclipselink.logging.level"                 -> logLevel,
            "eclipselink.target-database"               -> productName,
            "jakarta.persistence.database-product-name" -> productName,
            "jakarta.persistence.jdbc.driver"           -> driver,
            "jakarta.persistence.jdbc.password"         -> password,
            "jakarta.persistence.jdbc.url"              -> url,
            "jakarta.persistence.jdbc.user"             -> user
        )
        apply(props)
    }

}
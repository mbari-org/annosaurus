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

import jakarta.persistence.EntityManagerFactory
import org.eclipse.persistence.config.TargetDatabase

object DerbyTestDAOFactory extends TestDAOFactory {

    override def testProps(): Map[String, String] =
        TestDAOFactory.TestProperties ++
            Map(
                "hibernate.dialect"                                     -> "org.hibernate.dialect.DerbyDialect",
                "hibernate.hbm2ddl.auto"                                -> "create",
                "jakarta.persistence.schema-generation.database.action" -> "create",
                "jakarta.persistence.schema-generation.scripts.action"  -> "drop-and-create"
            )


    lazy val entityManagerFactory: EntityManagerFactory = {
        val driver   = config.getString("org.mbari.vars.annotation.database.derby.driver")
        val url      = config.getString("org.mbari.vars.annotation.database.derby.url")
        val user     = config.getString("org.mbari.vars.annotation.database.derby.user")
        val password = config.getString("org.mbari.vars.annotation.database.derby.password")
        Class.forName(driver)
        EntityManagerFactories(url, user, password, driver, testProps())
    }
}
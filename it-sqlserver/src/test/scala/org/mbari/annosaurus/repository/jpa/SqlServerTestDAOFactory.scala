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
import org.mbari.annosaurus.DatabaseConfig
import org.mbari.annosaurus.etc.tc.AzureSqlEdgeContainerProvider
import org.testcontainers.containers.MSSQLServerContainer
import org.testcontainers.utility.DockerImageName

object SqlServerTestDAOFactory extends TestDAOFactory:

    // val container = new AzureSqlEdgeContainerProvider().newInstance()

    // The image name must match the one in src/test/resources/container-license-acceptance.txt
    val container = new MSSQLServerContainer(DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest"))
    container.acceptLicense()
    container.withInitScript("sql/mssqlserver/02_m3_annotations.sql")
    container.withReuse(true)
    container.start()

//  override def beforeAll(): Unit = container.start()
    // NOTE: calling container.stop() after each test causes the tests to lose the connection to the database.
    // I'm using a shutdown hook to close the container at the end of the tests.
    //  override def afterAll(): Unit  = container.stop()
    Runtime.getRuntime.addShutdownHook(new Thread(() => container.stop()))

    override def testProps(): Map[String, String] =
        TestDAOFactory.TestProperties ++
            Map(
                "hibernate.dialect"            -> "org.hibernate.dialect.SQLServerDialect",
                "hibernate.hbm2ddl.auto"       -> "validate",
                "hibernate.hikari.idleTimeout" -> "1000",
                "hibernate.hikari.maxLifetime" -> "3000",
                "jakarta.persistence.schema-generation.scripts.action" -> "drop-and-create"
            )

    lazy val entityManagerFactory: EntityManagerFactory =
        val driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
        Class.forName(driver)
        EntityManagerFactories(
            container.getJdbcUrl(),
            container.getUsername(),
            container.getPassword(),
            container.getDriverClassName(),
            testProps()
        )

    lazy val databaseConfig: DatabaseConfig = DatabaseConfig(
        container.getJdbcUrl(),
        container.getUsername(),
        container.getPassword(),
        container.getDriverClassName(),
        annotationView
    )

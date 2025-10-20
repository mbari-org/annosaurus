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

package org.mbari.annosaurus.etc.flyway

import org.flywaydb.core.Flyway
import org.mbari.annosaurus.DatabaseConfig
import org.mbari.annosaurus.etc.jdk.Loggers.given

import scala.util.Try
import org.mbari.annosaurus.etc.jdbc.Databases

object FlywayMigrator:

    private val log = System.getLogger(getClass.getName)

    def migrate(databaseConfig: DatabaseConfig): Either[Throwable, Unit] =
        // Implementation of Flyway migration logic
        Try {
            val databaseType = Databases.typeFromUrl(databaseConfig.url)
            val location     = databaseType match
                case Databases.DatabaseType.SQLServer  => "classpath:/db/migrations/sqlserver"
                case Databases.DatabaseType.PostgreSQL => "classpath:/db/migrations/postgres"
                case _                                 => throw new IllegalArgumentException(s"Unsupported database type: $databaseType")

            log.atDebug.log(s"Starting database migrations on ${databaseConfig.url}")
            val flyway = Flyway
                .configure()
                .table("schema_history_annosaurus") // name of the metadata table
                .locations(location)         // migration scripts location
                .dataSource(databaseConfig.url, databaseConfig.user, databaseConfig.password)
                .baselineOnMigrate(true)     // this makes Flyway baseline if no metadata table exists
                .load()

            val result = flyway.migrate()
            result.migrationsExecuted match
                case 0          => log.atInfo.log("No database migrations were necessary")
                case n if n > 0 => log.atInfo.log(s"Successfully applied $n database migrations")
                case _          => log.atWarn.log("Database migration result was unexpected")

            if !result.success then throw new Exception("Migration failed using SQL in " + location)

            log.atInfo
                .log(
                    "Flyway Database migrations applied. Current schema version: " + flyway.info().current().getVersion
                )
        }.toEither

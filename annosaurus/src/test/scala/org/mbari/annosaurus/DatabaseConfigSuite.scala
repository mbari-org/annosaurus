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

package org.mbari.annosaurus

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.mbari.annosaurus.etc.flyway.FlywayMigrator
import org.mbari.annosaurus.etc.jdbc.Databases.DatabaseType

class DatabaseConfigSuite extends munit.FunSuite:

    private def configured(value: String): Option[String] =
        DatabaseConfig.videoReferencesTable(
            ConfigFactory.empty().withValue(DatabaseConfig.VideoReferencesTablePath, ConfigValueFactory.fromAnyRef(value))
        )

    test("an unset video references table is None") {
        assertEquals(DatabaseConfig.videoReferencesTable(ConfigFactory.empty()), None)
    }

    test("empty and whitespace-only video references tables are treated as unset") {
        assertEquals(configured(""), None)
        assertEquals(configured("   "), None)
        assertEquals(configured("\t"), None)
    }

    test("a real video references table is passed through (trimmed)") {
        assertEquals(configured("M3_VIDEO_ASSETS.dbo.video_references"), Some("M3_VIDEO_ASSETS.dbo.video_references"))
        assertEquals(configured("  video_references "), Some("video_references"))
    }

    test("blank values fall back to the dialect default in the Flyway placeholder") {
        for v <- Seq(None, Some(""), Some("   ")) do
            assertEquals(FlywayMigrator.resolveVideoReferencesTable(DatabaseType.SQLServer, v), "dbo.video_references")
            assertEquals(FlywayMigrator.resolveVideoReferencesTable(DatabaseType.PostgreSQL, v), "video_references")
        assertEquals(
            FlywayMigrator.resolveVideoReferencesTable(DatabaseType.SQLServer, Some("M3_VIDEO_ASSETS.dbo.video_references")),
            "M3_VIDEO_ASSETS.dbo.video_references"
        )
    }

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

class PostgresLocalizationMigrationSuite extends LocalizationMigrationSuite:
    given daoFactory: TestDAOFactory = PostgresTestDAOFactory

    override def createVideoReferencesTable: Seq[String] =
        Seq("CREATE TABLE video_references (uuid uuid PRIMARY KEY, width int, height int)")

    override def dropVideoReferencesTable: String = "DROP TABLE IF EXISTS video_references"

    override def routineSql: String = "SELECT outcome, n FROM migrate_localizations()"

    override def videoReferencesName: String = "video_references"

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

package org.mbari.annosaurus.repository

import org.mbari.annosaurus.repository.jpa.entity.IPersistentObject

import java.util.UUID

trait LocalizationDAO[T <: IPersistentObject] extends DAO[T]:

    def findByObservationUuid(uuid: UUID): Seq[T]

    def findByObservationUuidAndType(uuid: UUID, localizationType: String): Option[T]

    def findByVideoReferenceUuid(uuid: UUID, limit: Option[Int], offset: Option[Int]): Seq[T]

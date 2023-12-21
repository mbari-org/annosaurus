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

import jakarta.persistence.{Column, Convert, GeneratedValue, Id}
import org.mbari.annosaurus.PersistentObject

import java.util.UUID
import jakarta.persistence.GenerationType

/** Mixin that supports the UUID fields
  *
  * @author
  *   Brian Schlining
  * @since 2016-05-05T17:50:00
  */
trait HasUUID extends PersistentObject {

    @Id
    @Column(
        name = "uuid",
        nullable = false,
        updatable = false,
//        length = 36,
    )
    @GeneratedValue(strategy = GenerationType.UUID)
//    @Convert(converter = classOf[UUIDConverter])
    var uuid: UUID = _

    def primaryKey: Option[UUID] = Option(uuid)

}

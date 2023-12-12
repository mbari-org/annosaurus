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

import org.mbari.annosaurus.model.MutableImagedMoment

import java.util.UUID

/** Special DAO for fetching just the index information from the ImagedMomemnts
  *
  * @author
  *   Brian Schlining
  * @since 2019-02-08T08:53:00
  */
trait IndexDAO[T <: MutableImagedMoment] extends DAO[T] {

    def findByVideoReferenceUuid(
        videoReferenceUuid: UUID,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): Iterable[MutableImagedMoment]

}

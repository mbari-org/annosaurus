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

package org.mbari.vars.annotation.dao

import java.util.UUID

import org.mbari.vars.annotation.model.CachedVideoReferenceInfo

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-06-17T16:10:00
  */
trait CachedVideoReferenceInfoDAO[T <: CachedVideoReferenceInfo] extends DAO[T] {

  def findByVideoReferenceUUID(uuid: UUID): Option[CachedVideoReferenceInfo]
  def findByPlatformName(platformName: String): Iterable[CachedVideoReferenceInfo]
  def findByMissionID(missionID: String): Iterable[CachedVideoReferenceInfo]
  def findByMissionContact(missionContact: String): Iterable[CachedVideoReferenceInfo]
  def findAllVideoReferenceUUIDs(): Iterable[UUID]
  def findAllMissionContacts(): Iterable[String]
  def findAllPlatformNames(): Iterable[String]
  def findAllMissionIDs(): Iterable[String]

}

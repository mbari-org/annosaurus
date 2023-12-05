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

package org.mbari.vars.annotation.model.simple

import java.util
import java.util.{UUID, List => JList}

import com.google.gson.annotations.Expose

import scala.jdk.CollectionConverters._

/**
  * @author Brian Schlining
  * @since 2019-06-05T13:46:00
  */
class ConceptAssociationRequest {

  @Expose(serialize = true)
  var linkName: String = _

  @Expose(serialize = true)
  var videoReferenceUuids: JList[UUID] = new util.ArrayList[UUID]

  def uuids: List[UUID] = videoReferenceUuids.asScala.toList

}

object ConceptAssociationRequest {
  def apply(linkName: String, videoReferenceUuids: Seq[UUID]): ConceptAssociationRequest = {
    val car = new ConceptAssociationRequest
    car.linkName = linkName
    videoReferenceUuids.foreach(uuid => car.videoReferenceUuids.add(uuid))
    car
  }
}

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

import org.mbari.vars.annotation.model._

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-07-11T14:41:00
  */
object Implicits {

  implicit def toCaseClass(association: Association): SimpleAssociation =
    SimpleAssociation(association)

  implicit def toCaseClass(observation: Observation): SimpleObservation =
    SimpleObservation(observation)

  implicit def toCaseClass(imagedMoment: ImagedMoment): SimpleImagedMoment =
    SimpleImagedMoment(imagedMoment)

  implicit def toCaseClass(imageReference: ImageReference): SimpleImageReference =
    SimpleImageReference(imageReference)

  implicit def toCaseClass(ancillaryDatum: CachedAncillaryDatum): SimpleAncillaryDatum =
    SimpleAncillaryDatum(ancillaryDatum)

  implicit def toCaseClass(videoReferenceInfo: CachedVideoReferenceInfo): SimpleVideoReferenceInfo =
    SimpleVideoReferenceInfo(videoReferenceInfo)

  implicit class RichAssociation(association: Association) {
    def asCase: SimpleAssociation = toCaseClass(association)
  }

  implicit class RichObservation(observation: Observation) {
    def asCase: SimpleObservation = toCaseClass(observation)
  }

  implicit class RichImagedMoment(imagedMoment: ImagedMoment) {
    def asCase: SimpleImagedMoment = toCaseClass(imagedMoment)
  }

  implicit class RichAncillaryDatum(ancillaryDatum: CachedAncillaryDatum) {
    def asCase: SimpleAncillaryDatum = toCaseClass(ancillaryDatum)
  }

  implicit class RichVideoReferenceInfo(videoReferenceInfo: CachedVideoReferenceInfo) {
    def asCase: SimpleVideoReferenceInfo = toCaseClass(videoReferenceInfo)
  }

}

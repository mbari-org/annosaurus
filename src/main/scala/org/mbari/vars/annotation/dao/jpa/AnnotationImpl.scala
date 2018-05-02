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

package org.mbari.vars.annotation.dao.jpa

import java.time.{ Duration, Instant }
import java.util.{ UUID, ArrayList => JArrayList, List => JList }

import com.google.gson.annotations.{ Expose, SerializedName }
import org.mbari.vars.annotation.model._
import org.mbari.vcr4j.time.Timecode

import scala.collection.JavaConverters._

/**
 * Simplified view of the data.
 *
 * @author Brian Schlining
 * @since 2016-07-12T10:00:00
 */
class AnnotationImpl extends Annotation {

  @Expose(serialize = true)
  var observationUuid: UUID = _

  @Expose(serialize = true)
  var concept: String = _

  @Expose(serialize = true)
  var observer: String = _

  @Expose(serialize = true)
  var observationTimestamp: Instant = _

  @Expose(serialize = true)
  var videoReferenceUuid: UUID = _

  @Expose(serialize = true)
  var imagedMomentUuid: UUID = _

  @Expose(serialize = true)
  var timecode: Timecode = _

  @Expose(serialize = true)
  @SerializedName(value = "elapsed_time_millis")
  var elapsedTime: Duration = _

  @Expose(serialize = true)
  var recordedTimestamp: Instant = _

  @Expose(serialize = true)
  @SerializedName(value = "duration_millis")
  var duration: Duration = _

  @Expose(serialize = true)
  var group: String = _

  @Expose(serialize = true)
  var activity: String = _

  @Expose(serialize = true)
  @SerializedName(value = "associations")
  protected var javaAssociations: JList[AssociationImpl] = new JArrayList[AssociationImpl]()
  def associations: Seq[Association] = javaAssociations.asScala
  def associations_=(as: Seq[Association]): Unit = {
    javaAssociations = as.map({
      case a: AssociationImpl => a
      case v: Association => AssociationImpl(v)
    }).asJava
  }

  @Expose(serialize = true)
  @SerializedName(value = "image_references")
  var javaImageReferences: JList[ImageReferenceImpl] = new JArrayList[ImageReferenceImpl]()
  def imageReferences: Seq[ImageReference] = javaImageReferences.asScala
  def imageReferences_=(irs: Seq[ImageReference]): Unit = {
    javaImageReferences = irs.map({
      case i: ImageReferenceImpl => i
      case v: ImageReference => ImageReferenceImpl(v)
    }).asJava
  }

}

object AnnotationImpl {

  def apply(observation: Observation): AnnotationImpl = {
    val a = new AnnotationImpl
    a.observationUuid = observation.uuid
    a.concept = observation.concept
    a.observer = observation.observer
    a.observationTimestamp = observation.observationDate
    a.videoReferenceUuid = observation.imagedMoment.videoReferenceUUID
    a.imagedMomentUuid = observation.imagedMoment.uuid
    a.timecode = observation.imagedMoment.timecode
    a.elapsedTime = observation.imagedMoment.elapsedTime
    a.recordedTimestamp = observation.imagedMoment.recordedDate
    a.duration = observation.duration
    a.group = observation.group
    a.activity = observation.activity
    a.associations = observation.associations.toSeq
    a.imageReferences = observation.imagedMoment.imageReferences.toSeq
    a
  }

  def apply(imagedMoment: ImagedMoment): Iterable[AnnotationImpl] = {
    imagedMoment.observations
      .map(apply)
  }

  def apply(
    videoReferenceUUID: UUID,
    concept: String,
    observer: String,
    observationDate: Instant = Instant.now(),
    timecode: Option[Timecode] = None,
    elapsedTime: Option[Duration] = None,
    recordedDate: Option[Instant] = None,
    duration: Option[Duration] = None,
    group: Option[String] = None,
    activity: Option[String] = None): AnnotationImpl = {

    val annotation = new AnnotationImpl
    annotation.videoReferenceUuid = videoReferenceUUID
    annotation.concept = concept
    annotation.observer = observer
    annotation.observationTimestamp = observationDate
    timecode.foreach(annotation.timecode = _)
    elapsedTime.foreach(annotation.elapsedTime = _)
    recordedDate.foreach(annotation.recordedTimestamp = _)
    duration.foreach(annotation.duration = _)
    group.foreach(annotation.group = _)
    activity.foreach(annotation.activity = _)
    annotation
  }

}


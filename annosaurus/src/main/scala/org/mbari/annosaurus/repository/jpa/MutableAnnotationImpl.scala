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

import java.time.{Duration, Instant}
import java.util.{ArrayList => JArrayList, List => JList, UUID}
import com.google.gson.annotations.{Expose, SerializedName}
import org.mbari.annosaurus.model.{
    MutableAnnotation,
    MutableAssociation,
    MutableImageReference,
    MutableImagedMoment,
    MutableObservation
}
import org.mbari.annosaurus.repository.jpa.entity.{AssociationEntity, ImageReferenceEntity}
import org.mbari.vcr4j.time.Timecode

import scala.jdk.CollectionConverters._

/** Simplified view of the data.
  *
  * @author
  *   Brian Schlining
  * @since 2016-07-12T10:00:00
  */
class MutableAnnotationImpl extends MutableAnnotation {

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
    var javaAssociations: JList[AssociationEntity]        =
        new JArrayList[AssociationEntity]()
    def associations: Seq[MutableAssociation]             = javaAssociations.asScala.toSeq
    def associations_=(as: Seq[MutableAssociation]): Unit = {
        javaAssociations = as.map {
            case a: AssociationEntity  => a
            case v: MutableAssociation => AssociationEntity(v)
        }.asJava
    }

    @Expose(serialize = true)
    @SerializedName(value = "image_references")
    var javaImageReferences: JList[ImageReferenceEntity]         =
        new JArrayList[ImageReferenceEntity]()
    def imageReferences: Seq[MutableImageReference]              = javaImageReferences.asScala.toSeq
    def imageReferences_=(irs: Seq[MutableImageReference]): Unit = {
        javaImageReferences = irs.map {
            case i: ImageReferenceEntity  => i
            case v: MutableImageReference => ImageReferenceEntity(v)
        }.asJava
    }

    override def toString =
        s"MutableAnnotationImpl($concept, $observer, $observationTimestamp)"
}

object MutableAnnotationImpl {

    def apply(observation: MutableObservation): MutableAnnotationImpl = {
        val a = new MutableAnnotationImpl
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

    def apply(imagedMoment: MutableImagedMoment): Iterable[MutableAnnotationImpl] = {
        imagedMoment
            .observations
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
        activity: Option[String] = None
    ): MutableAnnotationImpl = {

        val annotation = new MutableAnnotationImpl
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

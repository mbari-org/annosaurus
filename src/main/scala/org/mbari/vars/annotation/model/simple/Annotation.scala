package org.mbari.vars.annotation.model.simple

import java.time.{ Duration, Instant }
import java.util.UUID

import com.google.gson.annotations.{ Expose, SerializedName }
import org.mbari.vars.annotation.model.{ Association, ImageReference, Observation }
import org.mbari.vcr4j.time.Timecode
import java.util.{ ArrayList => JArrayList, List => JList }

import scala.collection.JavaConverters._

/**
 * Simplified view of the data.
 *
 * @author Brian Schlining
 * @since 2016-07-12T10:00:00
 */
class Annotation {

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
  protected var javaAssociations: JList[Association] = new JArrayList[Association]()
  def associations: Seq[Association] = javaAssociations.asScala
  def associations_=(as: Seq[Association]): Unit = {
    javaAssociations = as.asJava
  }

  @Expose(serialize = true)
  @SerializedName(value = "image_references")
  var javaImageReferences: JList[ImageReference] = new JArrayList[ImageReference]()
  def imageReferences: Seq[ImageReference] = javaImageReferences.asScala
  def imageReferences_=(irs: Seq[ImageReference]): Unit = {
    javaImageReferences = irs.asJava
  }

}

object Annotation {

  def apply(observation: Observation): Annotation = {
    val a = new Annotation
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

  def apply(videoReferenceUUID: UUID,
            concept: String,
            observer: String,
            observationDate: Instant = Instant.now(),
            timecode: Option[Timecode] = None,
            elapsedTime: Option[Duration] = None,
            recordedDate: Option[Instant] = None,
            duration: Option[Duration] = None,
            group: Option[String] = None,
            activity: Option[String] = None): Annotation = {

    val annotation = new Annotation
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


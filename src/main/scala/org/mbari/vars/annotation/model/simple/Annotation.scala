package org.mbari.vars.annotation.model.simple

import java.time.{ Duration, Instant }
import java.util.UUID

import com.google.gson.annotations.{ Expose, SerializedName }
import org.mbari.vars.annotation.model.{ Association, Observation }
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
  @SerializedName(value = "associations")
  protected var javaAssociations: JList[SimpleAssociation] = new JArrayList[SimpleAssociation]()
  def associations: Seq[SimpleAssociation] = javaAssociations.asScala
  def associations_=(as: Seq[SimpleAssociation]): Unit = {
    javaAssociations = as.asJava
  }

  @Expose(serialize = true)
  @SerializedName(value = "image_references")
  var javaImageReferences: JList[SimpleImageReference] = new JArrayList[SimpleImageReference]()
  def imageReferences: Seq[SimpleImageReference] = javaImageReferences.asScala
  def imageReferences_=(irs: Seq[SimpleImageReference]): Unit = {
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
    a.timecode = observation.imagedMoment.timecode
    a.elapsedTime = observation.imagedMoment.elapsedTime
    a.recordedTimestamp = observation.imagedMoment.recordedDate
    a.duration = observation.duration
    a.group = observation.group
    a.associations = observation.associations.map(SimpleAssociation(_)).toSeq
    a.imageReferences = observation.imagedMoment.imageReferences.map(SimpleImageReference(_)).toSeq
    a
  }

}


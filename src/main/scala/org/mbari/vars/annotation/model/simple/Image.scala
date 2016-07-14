package org.mbari.vars.annotation.model.simple

import java.net.URL
import java.time.{ Duration, Instant }
import java.util.UUID

import com.google.gson.annotations.{ Expose, SerializedName }
import org.mbari.vars.annotation.model.ImageReference
import org.mbari.vcr4j.time.Timecode

/**
 * Created by brian on 7/14/16.
 */
class Image {

  @Expose(serialize = true)
  var imageReferenceUuid: UUID = _

  @Expose(serialize = true)
  var format: String = _

  @Expose(serialize = true)
  var width: Int = _

  @Expose(serialize = true)
  var height: Int = _

  @Expose(serialize = true)
  var url: URL = _

  @Expose(serialize = true)
  var description: String = _

  @Expose(serialize = true)
  var videoReferenceUuid: UUID = _

  @Expose(serialize = true)
  var timecode: Timecode = _

  @Expose(serialize = true)
  @SerializedName(value = "elapsed_time_millis")
  var elapsedTime: Duration = _

  @Expose(serialize = true)
  var recordedTimestamp: Instant = _

}

object Image {

  def apply(imageReference: ImageReference): Image = {
    val i = new Image
    i.imageReferenceUuid = imageReference.uuid
    i.format = imageReference.format
    i.width = imageReference.width
    i.height = imageReference.height
    i.url = imageReference.url
    i.description = imageReference.description
    i.videoReferenceUuid = imageReference.imagedMoment.videoReferenceUUID
    i.timecode = imageReference.imagedMoment.timecode
    i.elapsedTime = imageReference.imagedMoment.elapsedTime
    i.recordedTimestamp = imageReference.imagedMoment.recordedDate
    i
  }
}

package org.mbari.vars.annotation.model.simple


import java.time.Duration
import java.util
import java.util.{UUID, List => JList}

import com.google.gson.annotations.Expose

import scala.collection.JavaConverters._
/**
 * @author Brian Schlining
 * @since 2019-09-12T14:27:00
 */
class WindowRequest {

  @Expose(serialize = true)
  var videoReferenceUuids: JList[UUID] = new util.ArrayList[UUID]()

  def uuids: List[UUID] = videoReferenceUuids.asScala.toList

  @Expose(serialize = true)
  var imagedMomentUuid: UUID = _

  @Expose(serialize = true)
  var window: Duration = _

}

object WindowRequest {
  def apply(videoReferenceUuids: Seq[UUID], imagedMomentUuid: UUID, window: Duration): WindowRequest = {
    val wr = new WindowRequest
    videoReferenceUuids.foreach(uuid => wr.videoReferenceUuids.add(uuid))
    wr.imagedMomentUuid = imagedMomentUuid
    wr.window = window
    wr
  }
}

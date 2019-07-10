package org.mbari.vars.annotation.model.simple

import java.util
import java.util.{UUID, List => JList}

import scala.collection.JavaConverters._

import com.google.gson.annotations.Expose

/**
  * Data class for information needed to request all annotations from multiple
  * videos in a single REST call
 * @author Brian Schlining
 * @since 2019-07-10T14:10:00
 */
class MultiRequest {

  @Expose(serialize = true)
  var videoReferenceUuids: JList[UUID] = new util.ArrayList[UUID]()

  def uuids: List[UUID] = videoReferenceUuids.asScala.toList

}

object MultiRequest {
  def apply(videoReferenceUuids: Seq[UUID]): MultiRequest = {
    val mr = new MultiRequest
    videoReferenceUuids.foreach(uuid => mr.videoReferenceUuids.add(uuid))
    mr
  }
}

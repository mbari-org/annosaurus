package org.mbari.vars.annotation.model.simple

import java.util
import java.util.{UUID, List => JList}

import com.google.gson.annotations.Expose

import scala.collection.JavaConverters._

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

package org.mbari.vars.annotation.model.simple

import java.util.UUID

import com.google.gson.annotations.Expose

class ConceptAssociation {

  /**
  * This is the association UUID
    */
  @Expose(serialize = true)
  var uuid: UUID = _

  @Expose(serialize = true)
  var videoReferenceUuid: UUID = _

  @Expose(serialize = true)
  var concept: String = _

  @Expose(serialize = true)
  var linkName: String = _

  @Expose(serialize = true)
  var toConcept: String = _

  @Expose(serialize = true)
  var linkValue: String = _

  @Expose(serialize = true)
  var mimeType: String = _

}

object ConceptAssociation {
  def apply(uuid: UUID, videoReferenceUuid: UUID, concept: String, linkName: String,
            toConcept: String, linkValue: String, mimeType: String):  ConceptAssociation = {
    val ca = new ConceptAssociation
    ca.uuid = uuid
    ca.videoReferenceUuid = videoReferenceUuid
    ca.concept = concept
    ca.linkName = linkName
    ca.toConcept = toConcept
    ca.linkValue = linkValue
    ca.mimeType = mimeType
    ca
  }
}

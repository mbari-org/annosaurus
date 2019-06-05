package org.mbari.vars.annotation.model.simple

import java.util.UUID

import com.google.gson.annotations.Expose

class ConceptAssociation {

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

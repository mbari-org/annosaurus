package org.mbari.vars.annotation.model.simple

import java.util
import java.util.{UUID, List => JList}

import com.google.gson.annotations.Expose
import scala.collection.JavaConverters._

/**
 * @author Brian Schlining
 * @since 2019-06-05T13:55:00
 */
class ConceptAssociationResponse {

  @Expose(serialize = true)
  var conceptAssociationRequest: ConceptAssociationRequest = _

  @Expose(serialize = true)
  var conceptAssociations: JList[ConceptAssociation] = new util.ArrayList[ConceptAssociation]

  def associations: List[ConceptAssociation] = conceptAssociations.asScala.toList

}

object ConceptAssociationResponse {
  def apply(request: ConceptAssociationRequest, conceptAssociations: Seq[ConceptAssociation]): ConceptAssociationResponse = {
    val car = new ConceptAssociationResponse
    car.conceptAssociationRequest = request
    conceptAssociations.foreach(ca => car.conceptAssociations.add(ca))
    car
  }
}

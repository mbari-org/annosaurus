package org.mbari.vars.annotation.model.simple

import org.mbari.vars.annotation.model._

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-07-11T14:41:00
 */
object Implicits {

  implicit def toCaseClass(association: Association): SimpleAssociation =
    SimpleAssociation(association)

  implicit def toCaseClass(observation: Observation): SimpleObservation =
    SimpleObservation(observation)

  implicit def toCaseClass(imagedMoment: ImagedMoment): SimpleImagedMoment =
    SimpleImagedMoment(imagedMoment)

  implicit def toCaseClass(imageReference: ImageReference): SimpleImageReference =
    SimpleImageReference(imageReference)

  implicit def toCaseClass(ancillaryDatum: CachedAncillaryDatum): SimpleAncillaryDatum =
    SimpleAncillaryDatum(ancillaryDatum)

  implicit def toCaseClass(videoReferenceInfo: CachedVideoReferenceInfo): SimpleVideoReferenceInfo =
    SimpleVideoReferenceInfo(videoReferenceInfo)

  implicit class RichAssociation(association: Association) {
    def asCase: SimpleAssociation = toCaseClass(association)
  }

  implicit class RichObservation(observation: Observation) {
    def asCase: SimpleObservation = toCaseClass(observation)
  }

  implicit class RichImagedMoment(imagedMoment: ImagedMoment) {
    def asCase: SimpleImagedMoment = toCaseClass(imagedMoment)
  }

  implicit class RichAncillaryDatum(ancillaryDatum: CachedAncillaryDatum) {
    def asCase: SimpleAncillaryDatum = toCaseClass(ancillaryDatum)
  }

  implicit class RichVideoReferenceInfo(videoReferenceInfo: CachedVideoReferenceInfo) {
    def asCase: SimpleVideoReferenceInfo = toCaseClass(videoReferenceInfo)
  }

}

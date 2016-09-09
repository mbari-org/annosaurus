package org.mbari.vars.annotation.dao

import org.mbari.vars.annotation.model._

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-16T14:04:00
 */
trait DAOFactory[IM <: ImagedMoment, OBS <: Observation, A <: Association, IR <: ImageReference, CAD <: CachedAncillaryDatum, CMI <: CachedVideoReferenceInfo] {

  def newAssociationDAO(): AssociationDAO[A]
  def newAssociationDAO(dao: DAO[_]): AssociationDAO[A]

  def newImageReferenceDAO(): ImageReferenceDAO[IR]
  def newImageReferenceDAO(dao: DAO[_]): ImageReferenceDAO[IR]

  def newImagedMomentDAO(): ImagedMomentDAO[IM]
  def newImagedMomentDAO(dao: DAO[_]): ImagedMomentDAO[IM]

  def newCachedAncillaryDatumDAO(): CachedAncillaryDatumDAO[CAD]
  def newCachedAncillaryDatumDAO(dao: DAO[_]): CachedAncillaryDatumDAO[CAD]

  def newCachedVideoReferenceInfoDAO(): CachedVideoReferenceInfoDAO[CMI]
  def newCachedVideoReferenceInfoDAO(dao: DAO[_]): CachedVideoReferenceInfoDAO[CMI]

  def newObservationDAO(): ObservationDAO[OBS]
  def newObservationDAO(dao: DAO[_]): ObservationDAO[OBS]

}

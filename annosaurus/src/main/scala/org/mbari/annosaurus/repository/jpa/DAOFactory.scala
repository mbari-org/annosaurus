/*
 * Copyright 2017 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.annosaurus.repository.jpa

import org.mbari.annosaurus.model._
import org.mbari.annosaurus.repository.{AssociationDAO, CachedAncillaryDatumDAO, CachedVideoReferenceInfoDAO, DAO, ImageReferenceDAO, ImagedMomentDAO, IndexDAO, ObservationDAO}

/** @author
  *   Brian Schlining
  * @since 2016-06-16T14:04:00
  */
trait DAOFactory[
    IM <: ImagedMoment,
    OBS <: MutableObservation,
    A <: MutableAssociation,
    IR <: MutableImageReference,
    CAD <: CachedAncillaryDatum,
    CMI <: CachedVideoReferenceInfo,
    ID <: ImagedMoment
] {

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

    def newIndexDAO(): IndexDAO[ID]
    def newIndexDAO(dao: DAO[_]): IndexDAO[ID]

}

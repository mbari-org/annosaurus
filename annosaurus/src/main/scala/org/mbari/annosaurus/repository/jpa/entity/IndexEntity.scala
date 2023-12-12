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

package org.mbari.annosaurus.repository.jpa.entity

import com.google.gson.annotations.{Expose, SerializedName}
import jakarta.persistence._
import org.mbari.annosaurus.Constants
import org.mbari.annosaurus.model.{CachedAncillaryDatum, MutableImageReference, ImagedMoment, MutableObservation}
import org.mbari.annosaurus.repository.jpa._
import org.mbari.vcr4j.time.Timecode

import java.time.{Duration, Instant}
import java.util.UUID

/**
  * @author Brian Schlining
  * @since 2019-02-08T10:15:00
  */
@Entity(name = "Index")
@Table(name = "imaged_moments")
@EntityListeners(value = Array(classOf[TransactionLogger]))
@NamedQueries(
  Array(
    new NamedQuery(
      name = "Index.findByVideoReferenceUUID",
      query = "SELECT i FROM Index i WHERE i.videoReferenceUUID = :uuid ORDER BY i.uuid"
    )
  )
)
class IndexEntity extends ImagedMoment with JPAPersistentObject {

  @Expose(serialize = true)
  @SerializedName(value = "video_reference_uuid")
  @Column(name = "video_reference_uuid", nullable = true, columnDefinition = "CHAR(36)")
  @Convert(converter = classOf[UUIDConverter])
  var videoReferenceUUID: UUID = _

  @Expose(serialize = true)
  @Column(name = "elapsed_time_millis", nullable = true)
  @Convert(converter = classOf[DurationConverter])
  var elapsedTime: Duration = _

  @Expose(serialize = true)
  @Column(name = "recorded_timestamp", nullable = true)
  @Temporal(value = TemporalType.TIMESTAMP)
  @Convert(converter = classOf[InstantConverter])
  var recordedDate: Instant = _

  @Expose(serialize = true)
  @Column(name = "timecode", nullable = true)
  @Convert(converter = classOf[TimecodeConverter])
  var timecode: Timecode = _

  //  @Expose(serialize = true)
  //  @SerializedName(value = "ancillary_data")
  //  @OneToOne(
  //    mappedBy = "imagedMoment",
  //    cascade = Array(CascadeType.ALL),
  //    optional = true,
  //    fetch = FetchType.LAZY,
  //    targetEntity = classOf[CachedAncillaryDatumImpl])
  //  protected var _ancillaryDatum: CachedAncillaryDatum = _
  //
  //  def ancillaryDatum: CachedAncillaryDatum = _ancillaryDatum
  //  def ancillaryDatum_=(ad: CachedAncillaryDatum): Unit = {
  //    if (_ancillaryDatum != null) _ancillaryDatum.imagedMoment = null
  //    _ancillaryDatum = ad
  //    ad.imagedMoment = this
  //  }

  override def toString: String = Constants.GSON.toJson(this)

  /* --- IGNORE these methods below --- */
  override def addObservation(observation: MutableObservation): Unit = ???

  override def removeObservation(observation: MutableObservation): Unit = ???

  @Expose(serialize = false)
  override def observations: Iterable[MutableObservation] = ???

  override def addImageReference(imageReference: MutableImageReference): Unit = ???

  override def removeImageReference(imageReference: MutableImageReference): Unit = ???

  @Expose(serialize = false)
  override def imageReferences: Iterable[MutableImageReference] = ???

  @Transient
  var ancillaryDatum: CachedAncillaryDatum = _

  override def primaryKey: Option[UUID] = ???
}
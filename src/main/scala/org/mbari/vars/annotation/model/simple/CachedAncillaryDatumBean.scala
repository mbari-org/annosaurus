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

package org.mbari.vars.annotation.model.simple

import java.time.Instant
import java.util.UUID

import com.google.gson.annotations.{ Expose, JsonAdapter }
import org.mbari.vars.annotation.gson.{ DoubleOptionDeserializer, FloatOptionDeserializer }
import org.mbari.vars.annotation.model.{ CachedAncillaryDatum, ImagedMoment }

/**
 * @author Brian Schlining
 * @since 2017-11-09T12:55:00
 */
class CachedAncillaryDatumBean extends CachedAncillaryDatum {

  @Expose(serialize = true)
  override var uuid: UUID = _

  override var imagedMoment: ImagedMoment = _

  @Expose(serialize = true)
  var imagedMomentUuid: UUID = _

  @Expose(serialize = true)
  var recordedTimestamp: Option[Instant] = _

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[DoubleOptionDeserializer])
  override var latitude: Option[Double] = _

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[DoubleOptionDeserializer])
  override var longitude: Option[Double] = _

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  override var depthMeters: Option[Float] = _

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  override var altitude: Option[Float] = _
  /**
   * Coordinate Reference System for latitude and longitude
   */
  @Expose(serialize = true)
  override var crs: String = _

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  override var salinity: Option[Float] = _

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  override var temperatureCelsius: Option[Float] = _

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  override var oxygenMlL: Option[Float] = _

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  override var pressureDbar: Option[Float] = _

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  override var lightTransmission: Option[Float] = _

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[DoubleOptionDeserializer])
  override var x: Option[Double] = _

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[DoubleOptionDeserializer])
  override var y: Option[Double] = _

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[DoubleOptionDeserializer])
  override var z: Option[Double] = _

  @Expose(serialize = true)
  override var posePositionUnits: String = _

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[DoubleOptionDeserializer])
  override var phi: Option[Double] = _

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[DoubleOptionDeserializer])
  override var theta: Option[Double] = _

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[DoubleOptionDeserializer])
  override var psi: Option[Double] = _

  override val lastUpdated: Option[Instant] = None

  override def primaryKey = Option(uuid)

}

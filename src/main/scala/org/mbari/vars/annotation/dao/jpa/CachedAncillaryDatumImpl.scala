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

package org.mbari.vars.annotation.dao.jpa

import java.lang.{ Double => JDouble, Float => JFloat }
import javax.persistence.{ CascadeType, JoinColumn, _ }

import com.google.gson.annotations.{ Expose, JsonAdapter }
import org.mbari.vars.annotation.gson.{ DoubleOptionDeserializer, FloatOptionDeserializer }
import org.mbari.vars.annotation.model.{ CachedAncillaryDatum, ImagedMoment }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T15:17:00
 */
@Entity(name = "AncillaryDatum")
@Table(name = "ancillary_data")
@EntityListeners(value = Array(classOf[TransactionLogger]))
@NamedQueries(Array(
  new NamedQuery(
    name = "AncillaryDatum.findAll",
    query = "SELECT a FROM AncillaryDatum a"
  ),
  new NamedQuery(
    name = "AncillaryDatum.findByImagedMomentUUID",
    query = "SELECT a FROM AncillaryDatum a JOIN a.imagedMoment i WHERE i.uuid = :uuid"
  ),
  new NamedQuery(
    name = "AncillaryDatum.findByObservationUUID",
    query = "SELECT a FROM AncillaryDatum a INNER JOIN FETCH a.imagedMoment im INNER JOIN FETCH im.javaObservations o WHERE o.uuid = :uuid"
  )
))
class CachedAncillaryDatumImpl extends CachedAncillaryDatum with JPAPersistentObject {

  @Expose(serialize = true)
  @Column(
    name = "coordinate_reference_system",
    length = 32,
    nullable = true
  )
  override var crs: String = _

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  @Column(
    name = "oxygen_ml_per_l",
    nullable = true
  )
  @Convert(converter = classOf[FloatOptionConverter])
  override var oxygenMlL: Option[Float] = None

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  @Column(
    name = "depth_meters",
    nullable = true
  )
  @Convert(converter = classOf[FloatOptionConverter])
  override var depthMeters: Option[Float] = None

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[DoubleOptionDeserializer])
  @Column(
    name = "z",
    nullable = true
  )
  @Convert(converter = classOf[DoubleOptionConverter])
  override var z: Option[Double] = None

  @Expose(serialize = true)
  @Column(
    name = "xyz_position_units",
    nullable = true
  )
  override var posePositionUnits: String = _

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  @Column(
    name = "latitude",
    nullable = true
  )
  @Convert(converter = classOf[DoubleOptionConverter])
  override var latitude: Option[Double] = None

  @Expose(serialize = false)
  @OneToOne(
    cascade = Array(CascadeType.PERSIST, CascadeType.DETACH),
    optional = false,
    targetEntity = classOf[ImagedMomentImpl]
  )
  @JoinColumn(name = "imaged_moment_uuid", nullable = false)
  override var imagedMoment: ImagedMoment = _

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  @Column(
    name = "y",
    nullable = true
  )
  @Convert(converter = classOf[DoubleOptionConverter])
  override var y: Option[Double] = None

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  @Column(
    name = "temperature_celsius",
    nullable = true
  )
  @Convert(converter = classOf[FloatOptionConverter])
  override var temperatureCelsius: Option[Float] = None

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  @Column(
    name = "x",
    nullable = true
  )
  @Convert(converter = classOf[DoubleOptionConverter])
  override var x: Option[Double] = None

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  @Column(
    name = "theta",
    nullable = true
  )
  @Convert(converter = classOf[DoubleOptionConverter])
  override var theta: Option[Double] = None

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  @Column(
    name = "longitude",
    nullable = true
  )
  @Convert(converter = classOf[DoubleOptionConverter])
  override var longitude: Option[Double] = None

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  @Column(
    name = "phi",
    nullable = true
  )
  @Convert(converter = classOf[DoubleOptionConverter])
  override var phi: Option[Double] = None

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  @Column(
    name = "psi",
    nullable = true
  )
  @Convert(converter = classOf[DoubleOptionConverter])
  override var psi: Option[Double] = None

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  @Column(
    name = "pressure_dbar",
    nullable = true
  )
  @Convert(converter = classOf[FloatOptionConverter])
  override var pressureDbar: Option[Float] = None

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  @Column(
    name = "salinity",
    nullable = true
  )
  @Convert(converter = classOf[FloatOptionConverter])
  override var salinity: Option[Float] = None

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  @Column(
    name = "altitude",
    nullable = true
  )
  @Convert(converter = classOf[FloatOptionConverter])
  override var altitude: Option[Float] = None

  @Expose(serialize = true)
  @JsonAdapter(value = classOf[FloatOptionDeserializer])
  @Column(
    name = "light_transmission",
    nullable = true
  )
  @Convert(converter = classOf[FloatOptionConverter])
  override var lightTransmission: Option[Float] = None
}

object CachedAncillaryDatumImpl {

  def apply(datum: CachedAncillaryDatum): CachedAncillaryDatumImpl = {
    datum match {
      case c: CachedAncillaryDatumImpl => c
      case c =>
        val d = new CachedAncillaryDatumImpl
        d.uuid = c.uuid
        d.imagedMoment = c.imagedMoment
        d.latitude = c.latitude
        d.longitude = c.longitude
        d.depthMeters = c.depthMeters
        d.altitude = c.altitude
        d.crs = c.crs
        d.salinity = c.salinity
        d.temperatureCelsius = c.temperatureCelsius
        d.oxygenMlL = c.oxygenMlL
        d.pressureDbar = c.pressureDbar
        d.lightTransmission = c.lightTransmission
        d.x = c.x
        d.y = c.y
        d.z = c.z
        d.posePositionUnits = c.posePositionUnits
        d.phi = c.phi
        d.theta = c.theta
        d.psi = c.psi
        d
    }
  }

  /**
   *
   * @param latitude
   * @param longitude
   * @param depthMeters
   * @return
   */
  def apply(latitude: Double, longitude: Double, depthMeters: Float): CachedAncillaryDatumImpl = {
    val d = new CachedAncillaryDatumImpl
    d.latitude = Option(latitude)
    d.longitude = Option(longitude)
    d.depthMeters = Option(depthMeters)
    d
  }

  def apply(
    latitude: Double,
    longitude: Double,
    depthMeters: Float,
    salinity: Float,
    temperatureCelsius: Float,
    pressureDbar: Float,
    oxygenMlL: Float,
    crs: String = "CRS:84"
  ): CachedAncillaryDatumImpl = {
    val d = apply(latitude, longitude, depthMeters)
    d.salinity = Option(salinity)
    d.temperatureCelsius = Option(temperatureCelsius)
    d.pressureDbar = Option(pressureDbar)
    d.oxygenMlL = Option(oxygenMlL)
    d.crs = crs
    d
  }

}
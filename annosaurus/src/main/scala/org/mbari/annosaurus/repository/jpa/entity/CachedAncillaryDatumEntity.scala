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

import com.google.gson.annotations.Expose
import jakarta.persistence._
import org.mbari.annosaurus.model.{MutableCachedAncillaryDatum, MutableImagedMoment}
import org.mbari.annosaurus.repository.jpa.{DoubleOptionConverter, JpaEntity, TransactionLogger}

/** @author
  *   Brian Schlining
  * @since 2016-06-17T15:17:00
  */
@Entity(name = "AncillaryDatum")
@Table(
    name = "ancillary_data",
    indexes = Array(
        new Index(
            name = "idx_ancillary_data__imaged_moment_uuid",
            columnList = "imaged_moment_uuid"
        ),
        new Index(
            name = "idx_ancillary_data__position",
            columnList = "latitude,longitude,depth_meters"
        )
    )
) //idx_ancillary_data_fk_im
@EntityListeners(value = Array(classOf[TransactionLogger]))
@NamedNativeQueries(
    Array(
        new NamedNativeQuery(
            name = "AncillaryDatum.deleteByVideoReferenceUuid",
            query =
                "DELETE FROM ancillary_data WHERE imaged_moment_uuid IN (SELECT uuid FROM imaged_moments WHERE video_reference_uuid = ?1)"
        )
    )
)
@NamedQueries(
    Array(
        new NamedQuery(
            name = "AncillaryDatum.findAll",
            query = "SELECT a FROM AncillaryDatum a ORDER BY a.uuid"
        ),
        new NamedQuery(
            name = "AncillaryDatum.findByImagedMomentUUID",
            query =
                "SELECT a FROM AncillaryDatum a JOIN a.imagedMoment i WHERE i.uuid = :uuid ORDER BY a.uuid"
        ),
        new NamedQuery(
            name = "AncillaryDatum.findByObservationUUID",
            query =
                "SELECT a FROM AncillaryDatum a INNER JOIN FETCH a.imagedMoment im INNER JOIN FETCH im.javaObservations o WHERE o.uuid = :uuid ORDER BY a.uuid"
        )
    )
)
class CachedAncillaryDatumEntity extends JpaEntity {

    @Expose(serialize = true)
    @Column(name = "coordinate_reference_system", length = 32, nullable = true)
    var crs: String = _

    @Expose(serialize = true)
    @Column(name = "oxygen_ml_per_l", nullable = true)
    @Convert(converter = classOf[DoubleOptionConverter])
    var oxygenMlL: Option[Double] = None

    @Expose(serialize = true)
    @Column(name = "depth_meters", nullable = true)
    @Convert(converter = classOf[DoubleOptionConverter])
    var depthMeters: Option[Double] = None

    @Expose(serialize = true)
    @Column(name = "z", nullable = true)
    @Convert(converter = classOf[DoubleOptionConverter])
    var z: Option[Double] = None

    @Expose(serialize = true)
    @Column(name = "xyz_position_units", nullable = true)
    var posePositionUnits: String = _

    @Expose(serialize = true)
    @Column(name = "latitude", nullable = true)
    @Convert(converter = classOf[DoubleOptionConverter])
    var latitude: Option[Double] = None

    @Expose(serialize = false)
    @OneToOne(
        cascade = Array(CascadeType.PERSIST, CascadeType.DETACH),
        optional = false,
        fetch = FetchType.LAZY,
        targetEntity = classOf[ImagedMomentEntity]
    )
    @JoinColumn(name = "imaged_moment_uuid", nullable = false, columnDefinition = "CHAR(36)")
    var imagedMoment: ImagedMomentEntity = _

    @Expose(serialize = true)
    @Column(name = "y", nullable = true)
    @Convert(converter = classOf[DoubleOptionConverter])
    var y: Option[Double] = None

    @Expose(serialize = true)
    @Column(name = "temperature_celsius", nullable = true)
    @Convert(converter = classOf[DoubleOptionConverter])
    var temperatureCelsius: Option[Double] = None

    @Expose(serialize = true)
    @Column(name = "x", nullable = true)
    @Convert(converter = classOf[DoubleOptionConverter])
    var x: Option[Double] = None

    @Expose(serialize = true)
    @Column(name = "theta", nullable = true)
    @Convert(converter = classOf[DoubleOptionConverter])
    var theta: Option[Double] = None

    @Expose(serialize = true)
    @Column(name = "longitude", nullable = true)
    @Convert(converter = classOf[DoubleOptionConverter])
    var longitude: Option[Double] = None

    @Expose(serialize = true)
    @Column(name = "phi", nullable = true)
    @Convert(converter = classOf[DoubleOptionConverter])
    var phi: Option[Double] = None

    @Expose(serialize = true)
    @Column(name = "psi", nullable = true)
    @Convert(converter = classOf[DoubleOptionConverter])
    var psi: Option[Double] = None

    @Expose(serialize = true)
    @Column(name = "pressure_dbar", nullable = true)
    @Convert(converter = classOf[DoubleOptionConverter])
    var pressureDbar: Option[Double] = None

    @Expose(serialize = true)
    @Column(name = "salinity", nullable = true)
    @Convert(converter = classOf[DoubleOptionConverter])
    var salinity: Option[Double] = None

    @Expose(serialize = true)
    @Column(name = "altitude", nullable = true)
    @Convert(converter = classOf[DoubleOptionConverter])
    var altitude: Option[Double] = None

    @Expose(serialize = true)
    @Column(name = "light_transmission", nullable = true)
    @Convert(converter = classOf[DoubleOptionConverter])
    var lightTransmission: Option[Double] = None
}

object CachedAncillaryDatumEntity {

    def apply(datum: MutableCachedAncillaryDatum): CachedAncillaryDatumEntity = {
        datum match {
            case c: CachedAncillaryDatumEntity => c
            case c                             =>
                val d = new CachedAncillaryDatumEntity
                d.uuid = c.uuid
                d.imagedMoment = ImagedMomentEntity(c.imagedMoment)
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

    /** @param latitude
      * @param longitude
      * @param depthMeters
      * @return
      */
    def apply(
        latitude: Double,
        longitude: Double,
        depthMeters: Float
    ): CachedAncillaryDatumEntity = {
        val d = new CachedAncillaryDatumEntity
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
    ): CachedAncillaryDatumEntity = {
        val d = apply(latitude, longitude, depthMeters)
        d.salinity = Option(salinity)
        d.temperatureCelsius = Option(temperatureCelsius)
        d.pressureDbar = Option(pressureDbar)
        d.oxygenMlL = Option(oxygenMlL)
        d.crs = crs
        d
    }

}

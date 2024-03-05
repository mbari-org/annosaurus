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

package org.mbari.annosaurus.repository.jpa.entity;

import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.UUID;
import org.mbari.annosaurus.repository.jpa.TransactionLogger;

@Entity(name = "AncillaryDatum")
@Table(
        name = "ancillary_data",
        indexes = {
                @Index(
                        name = "idx_ancillary_data__imaged_moment_uuid",
                        columnList = "imaged_moment_uuid"
                ),
                @Index(
                        name = "idx_ancillary_data__position",
                        columnList = "latitude,longitude,depth_meters"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ancillary_data__imaged_moment_uuid",
                        columnNames = {"imaged_moment_uuid"}
                )
        }
) //idx_ancillary_data_fk_im
@EntityListeners({TransactionLogger.class})
@NamedNativeQueries(
        {
                @NamedNativeQuery(
                        name = "AncillaryDatum.deleteByVideoReferenceUuid",
                        query =
                                "DELETE FROM ancillary_data WHERE imaged_moment_uuid IN (SELECT uuid FROM imaged_moments WHERE video_reference_uuid = ?1)"
                )
        }
)
@NamedQueries(
        {
                @NamedQuery(
                        name = "AncillaryDatum.findAll",
                        query = "SELECT a FROM AncillaryDatum a ORDER BY a.uuid"
                ),
                @NamedQuery(
                        name = "AncillaryDatum.findByImagedMomentUUID",
                        query =
                                "SELECT a FROM AncillaryDatum a JOIN a.imagedMoment i WHERE i.uuid = :uuid ORDER BY a.uuid"
                ),
                @NamedQuery(
                        name = "AncillaryDatum.findByObservationUUID",
                        query =
                                "SELECT a FROM AncillaryDatum a INNER JOIN FETCH a.imagedMoment im INNER JOIN FETCH im.observations o WHERE o.uuid = :uuid ORDER BY a.uuid"
                ),
                @NamedQuery(
                        name = "AncillaryDatum.findDTOByObservationUUID",
                        query =
                                "SELECT new org.mbari.annosaurus.repository.jpa.entity.AncillaryDatumDTO(a.latitude, a.longitude, a.depthMeters, a.altitude, a.crs, a.salinity, a.temperatureCelsius, a.oxygenMlL, a.pressureDbar, a.lightTransmission, a.x, a.y, a.z, a.posePositionUnits, a.phi, a.theta, a.psi, a.uuid, a.lastUpdatedTime, im.uuid, im.recordedTimestamp) FROM AncillaryDatum a JOIN a.imagedMoment im JOIN im.observations obs WHERE obs.uuid = :uuid ORDER BY a.uuid"
                ),
                @NamedQuery(
                        name = "AncillaryDatum.findDTOByVideoReferenceUUID",
                        query =
                                "SELECT new org.mbari.annosaurus.repository.jpa.entity.AncillaryDatumDTO(a.latitude, a.longitude, a.depthMeters, a.altitude, a.crs, a.salinity, a.temperatureCelsius, a.oxygenMlL, a.pressureDbar, a.lightTransmission, a.x, a.y, a.z, a.posePositionUnits, a.phi, a.theta, a.psi, a.uuid, a.lastUpdatedTime, im.uuid, im.recordedTimestamp) FROM AncillaryDatum a LEFT JOIN a.imagedMoment im WHERE im.videoReferenceUuid = :uuid ORDER BY a.uuid"
                ),
                @NamedQuery(
                        name = "AncillaryDatum.findDTOByImagedMomentUUID",
                        query =
                                "SELECT new org.mbari.annosaurus.repository.jpa.entity.AncillaryDatumDTO(a.latitude, a.longitude, a.depthMeters, a.altitude, a.crs, a.salinity, a.temperatureCelsius, a.oxygenMlL, a.pressureDbar, a.lightTransmission, a.x, a.y, a.z, a.posePositionUnits, a.phi, a.theta, a.psi, a.uuid, a.lastUpdatedTime, im.uuid, im.recordedTimestamp) FROM AncillaryDatum a LEFT JOIN a.imagedMoment im WHERE im.uuid = :uuid ORDER BY a.uuid"
                ),
                @NamedQuery(
                        name = "AncillaryDatum.findDTOByVidoeReferenceUUIDBetweenDates",
                        query =
                                "SELECT new org.mbari.annosaurus.repository.jpa.entity.AncillaryDatumDTO(a.latitude, a.longitude, a.depthMeters, a.altitude, a.crs, a.salinity, a.temperatureCelsius, a.oxygenMlL, a.pressureDbar, a.lightTransmission, a.x, a.y, a.z, a.posePositionUnits, a.phi, a.theta, a.psi, a.uuid, a.lastUpdatedTime, im.uuid, im.recordedTimestamp) FROM AncillaryDatum a LEFT JOIN a.imagedMoment im WHERE im.videoReferenceUuid = :uuid AND im.recordedTimestamp BETWEEN :start AND :end ORDER BY a.uuid"
                ),
                @NamedQuery(
                        name = "AncillaryDatum.findDTOByConcurrentRequest",
                        query =
                                "SELECT new org.mbari.annosaurus.repository.jpa.entity.AncillaryDatumDTO(a.latitude, a.longitude, a.depthMeters, a.altitude, a.crs, a.salinity, a.temperatureCelsius, a.oxygenMlL, a.pressureDbar, a.lightTransmission, a.x, a.y, a.z, a.posePositionUnits, a.phi, a.theta, a.psi, a.uuid, a.lastUpdatedTime, im.uuid, im.recordedTimestamp) FROM AncillaryDatum a LEFT JOIN a.imagedMoment im WHERE im.videoReferenceUuid IN :uuids AND im.recordedTimestamp BETWEEN :start AND :end ORDER BY a.uuid"
                ),
                @NamedQuery(
                        name = "AncillaryDatum.findDTOByMultiRequest",
                        query =
                                "SELECT new org.mbari.annosaurus.repository.jpa.entity.AncillaryDatumDTO(a.latitude, a.longitude, a.depthMeters, a.altitude, a.crs, a.salinity, a.temperatureCelsius, a.oxygenMlL, a.pressureDbar, a.lightTransmission, a.x, a.y, a.z, a.posePositionUnits, a.phi, a.theta, a.psi, a.uuid, a.lastUpdatedTime, im.uuid, im.recordedTimestamp) FROM AncillaryDatum a LEFT JOIN a.imagedMoment im WHERE im.videoReferenceUuid IN :uuids ORDER BY a.uuid"
                )
        }
)
public class CachedAncillaryDatumEntity implements IPersistentObject {

    @Id
    @Column(name = "uuid", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID uuid;

    /** Optimistic lock to prevent concurrent overwrites */
    @Version
    @Column(name = "last_updated_timestamp")
    protected Timestamp lastUpdatedTime;

    @Column(name = "coordinate_reference_system", length = 32, nullable = true)
    String crs;

    @Column(name = "oxygen_ml_per_l", nullable = true)
    Float oxygenMlL;

    @Column(name = "depth_meters", nullable = true)
    Float depthMeters;

    @Column(name = "z", nullable = true)
    Double z;

    @Column(name = "xyz_position_units", nullable = true)
    String posePositionUnits;

    @Column(name = "latitude", nullable = true)
    Double latitude;

    @OneToOne(
            cascade = {CascadeType.PERSIST, CascadeType.DETACH},
            optional = false,
            fetch = FetchType.LAZY,
            targetEntity = ImagedMomentEntity.class
    )
    @JoinColumn(
            name = "imaged_moment_uuid",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ancillary_data__imaged_moment_uuid")
    )
    ImagedMomentEntity imagedMoment;

    @Column(name = "y", nullable = true)
    Double y;

    @Column(name = "temperature_celsius", nullable = true)
    Float temperatureCelsius;

    @Column(name = "x", nullable = true)
    Double x;

    @Column(name = "theta", nullable = true)
    Double theta;

    @Column(name = "longitude", nullable = true)
    Double longitude;

    @Column(name = "phi", nullable = true)
    Double phi;

    @Column(name = "psi", nullable = true)
    Double psi;

    @Column(name = "pressure_dbar", nullable = true)
    Float pressureDbar;

    @Column(name = "salinity", nullable = true)
    Float salinity;

    @Column(name = "altitude", nullable = true)
    Float altitude;

    @Column(name = "light_transmission", nullable = true)
    Float lightTransmission;

    public CachedAncillaryDatumEntity() {
    }

    public CachedAncillaryDatumEntity(Double lat, Double lon, Float depth) {
        this.latitude = lat;
        this.longitude = lon;
        this.depthMeters = depth;
    }


    public CachedAncillaryDatumEntity(CachedAncillaryDatumEntity that) {
        this.altitude = that.altitude;
        this.crs = that.crs;
        this.depthMeters = that.depthMeters;
        this.lastUpdatedTime = that.lastUpdatedTime;
        this.latitude = that.latitude;
        this.lightTransmission = that.lightTransmission;
        this.longitude = that.longitude;
        this.oxygenMlL = that.oxygenMlL;
        this.phi = that.phi;
        this.posePositionUnits = that.posePositionUnits;
        this.pressureDbar = that.pressureDbar;
        this.psi = that.psi;
        this.salinity = that.salinity;
        this.temperatureCelsius = that.temperatureCelsius;
        this.theta = that.theta;
        this.uuid = that.uuid;
        this.x = that.x;
        this.y = that.y;
        this.z = that.z;
    }


    @Override
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Timestamp getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    public void setLastUpdatedTime(Timestamp lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }

    public String getCrs() {
        return crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }

    public Float getOxygenMlL() {
        return oxygenMlL;
    }

    public void setOxygenMlL(Float oxygenMlL) {
        this.oxygenMlL = oxygenMlL;
    }

    public Float getDepthMeters() {
        return depthMeters;
    }

    public void setDepthMeters(Float depthMeters) {
        this.depthMeters = depthMeters;
    }

    public Double getZ() {
        return z;
    }

    public void setZ(Double z) {
        this.z = z;
    }

    public String getPosePositionUnits() {
        return posePositionUnits;
    }

    public void setPosePositionUnits(String posePositionUnits) {
        this.posePositionUnits = posePositionUnits;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public ImagedMomentEntity getImagedMoment() {
        return imagedMoment;
    }

    public void setImagedMoment(ImagedMomentEntity imagedMoment) {
        this.imagedMoment = imagedMoment;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Float getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public void setTemperatureCelsius(Float temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getTheta() {
        return theta;
    }

    public void setTheta(Double theta) {
        this.theta = theta;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getPhi() {
        return phi;
    }

    public void setPhi(Double phi) {
        this.phi = phi;
    }

    public Double getPsi() {
        return psi;
    }

    public void setPsi(Double psi) {
        this.psi = psi;
    }

    public Float getPressureDbar() {
        return pressureDbar;
    }

    public void setPressureDbar(Float pressureDbar) {
        this.pressureDbar = pressureDbar;
    }

    public Float getSalinity() {
        return salinity;
    }

    public void setSalinity(Float salinity) {
        this.salinity = salinity;
    }

    public Float getAltitude() {
        return altitude;
    }

    public void setAltitude(Float altitude) {
        this.altitude = altitude;
    }

    public Float getLightTransmission() {
        return lightTransmission;
    }

    public void setLightTransmission(Float lightTransmission) {
        this.lightTransmission = lightTransmission;
    }

    @Override
    public String toString() {
        return "CachedAncillaryDatumEntity{" +
                "uuid=" + uuid +
                ", altitude=" + altitude +
                ", crs='" + crs + '\'' +
                ", depthMeters=" + depthMeters +
                ", latitude=" + latitude +
                ", lightTransmission=" + lightTransmission +
                ", longitude=" + longitude +
                ", oxygenMlL=" + oxygenMlL +
                ", phi=" + phi +
                ", posePositionUnits='" + posePositionUnits + '\'' +
                ", pressureDbar=" + pressureDbar +
                ", psi=" + psi +
                ", salinity=" + salinity +
                ", temperatureCelsius=" + temperatureCelsius +
                ", theta=" + theta +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}

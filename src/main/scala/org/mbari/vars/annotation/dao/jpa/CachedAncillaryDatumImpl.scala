package org.mbari.vars.annotation.dao.jpa

import javax.persistence.{ CascadeType, JoinColumn, _ }

import com.google.gson.annotations.Expose
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
    name = "Association.findByObservationUUID",
    query = "SELECT a FROM AncillaryDatum a INNER JOIN FETCH a.imagedMoment im INNER JOIN FETCH im.javaObservations o WHERE o.uuid = :uuid"
  )
))
class CachedAncillaryDatumImpl extends CachedAncillaryDatum with JPAPersistentObject {

  @Expose(serialize = true)
  @Column(
    name = "coordinate_reference_system",
    length = 32
  )
  override var crs: String = _

  @Expose(serialize = true)
  @Column(
    name = "oxygen_ml_per_l",
    nullable = true
  )
  override var oxygenMlL: Float = _

  @Expose(serialize = true)
  @Column(name = "depth_meters")
  override var depthMeters: Float = _

  @Expose(serialize = true)
  @Column(
    name = "z",
    nullable = true
  )
  override var z: Double = _

  @Expose(serialize = true)
  @Column(
    name = "xyz_position_units",
    nullable = true
  )
  override var posePositionUnits: String = _

  @Expose(serialize = true)
  @Column(
    name = "latitude",
    nullable = true
  )
  override var latitude: Double = _

  @Expose(serialize = false)
  @OneToOne(
    cascade = Array(CascadeType.PERSIST, CascadeType.DETACH),
    optional = false,
    targetEntity = classOf[ImagedMomentImpl]
  )
  @JoinColumn(name = "imaged_moment_uuid", nullable = false)
  override var imagedMoment: ImagedMoment = _

  @Expose(serialize = true)
  @Column(
    name = "y",
    nullable = true
  )
  override var y: Double = _

  @Expose(serialize = true)
  @Column(
    name = "temperature_celsius",
    nullable = true
  )
  override var temperatureCelsius: Float = _

  @Expose(serialize = true)
  @Column(
    name = "x",
    nullable = true
  )
  override var x: Double = _

  @Expose(serialize = true)
  @Column(
    name = "theta",
    nullable = true
  )
  override var theta: Double = _

  @Expose(serialize = true)
  @Column(
    name = "longitude",
    nullable = true
  )
  override var longitude: Double = _

  @Expose(serialize = true)
  @Column(
    name = "phi",
    nullable = true
  )
  override var phi: Double = _

  @Expose(serialize = true)
  @Column(
    name = "psi",
    nullable = true
  )
  override var psi: Double = _

  @Expose(serialize = true)
  @Column(
    name = "pressure_dbar",
    nullable = true
  )
  override var pressureDbar: Float = _

  @Expose(serialize = true)
  @Column(
    name = "salinity",
    nullable = true
  )
  override var salinity: Float = _

  @Expose(serialize = true)
  @Column(
    name = "altitude",
    nullable = true
  )
  override var altitude: Float = _

  @Expose(serialize = true)
  @Column(
    name = "light_transmission",
    nullable = true
  )
  override var lightTransmission: Float = _
}

object CachedAncillaryDatumImpl {

  /**
   *
   * @param latitude
   * @param longitude
   * @param depthMeters
   * @return
   */
  def apply(latitude: Double, longitude: Double, depthMeters: Float): CachedAncillaryDatumImpl = {
    val d = new CachedAncillaryDatumImpl
    d.latitude = latitude
    d.longitude = longitude
    d.depthMeters = depthMeters
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
    d.salinity = salinity
    d.temperatureCelsius = temperatureCelsius
    d.pressureDbar = pressureDbar
    d.oxygenMlL = oxygenMlL
    d.crs = crs
    d
  }

}
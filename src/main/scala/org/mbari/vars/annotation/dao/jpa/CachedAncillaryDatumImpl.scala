package org.mbari.vars.annotation.dao.jpa

import javax.persistence.{ CascadeType, JoinColumn, _ }

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
  )
))
class CachedAncillaryDatumImpl extends CachedAncillaryDatum with JPAPersistentObject {

  @Column(
    name = "coordinate_reference_system",
    length = 32
  )
  override var crs: String = _

  @Column(
    name = "oxygen_ml_per_l",
    nullable = true
  )
  override var oxygenMlL: Float = _

  @Column(name = "depth_meters")
  override var depthMeters: Float = _

  @Column(
    name = "z",
    nullable = true
  )
  override var z: Double = _

  @Column(
    name = "xyz_position_units",
    nullable = true
  )
  override var posePositionUnits: String = _

  @Column(
    name = "latitude",
    nullable = true
  )
  override var latitude: Double = _

  @OneToOne(
    cascade = Array(CascadeType.PERSIST, CascadeType.DETACH),
    optional = false,
    targetEntity = classOf[ImagedMomentImpl]
  )
  @JoinColumn(name = "imaged_moment_uuid", nullable = false)
  override var imagedMoment: ImagedMoment = _

  @Column(
    name = "y",
    nullable = true
  )
  override var y: Double = _

  @Column(
    name = "temperature_celsius",
    nullable = true
  )
  override var temperatureCelsius: Float = _

  @Column(
    name = "x",
    nullable = true
  )
  override var x: Double = _

  @Column(
    name = "theta",
    nullable = true
  )
  override var theta: Double = _

  @Column(
    name = "longitude",
    nullable = true
  )
  override var longitude: Double = _

  @Column(
    name = "phi",
    nullable = true
  )
  override var phi: Double = _

  @Column(
    name = "psi",
    nullable = true
  )
  override var psi: Double = _

  @Column(
    name = "pressure_dbar",
    nullable = true
  )
  override var pressureDbar: Float = _

  @Column(
    name = "salinity",
    nullable = true
  )
  override var salinity: Float = _

  @Column(
    name = "altitude",
    nullable = true
  )
  override var altitude: Float = _
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
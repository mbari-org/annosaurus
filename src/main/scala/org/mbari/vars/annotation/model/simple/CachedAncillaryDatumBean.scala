package org.mbari.vars.annotation.model.simple

import java.time.Instant
import java.util.UUID

import com.google.gson.annotations.Expose
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
  override var latitude: Option[Double] = _

  @Expose(serialize = true)
  override var longitude: Option[Double] = _

  @Expose(serialize = true)
  override var depthMeters: Option[Float] = _

  @Expose(serialize = true)
  override var altitude: Option[Float] = _
  /**
   * Coordinate Reference System for latitude and longitude
   */
  @Expose(serialize = true)
  override var crs: String = _

  @Expose(serialize = true)
  override var salinity: Option[Float] = _

  @Expose(serialize = true)
  override var temperatureCelsius: Option[Float] = _

  @Expose(serialize = true)
  override var oxygenMlL: Option[Float] = _

  @Expose(serialize = true)
  override var pressureDbar: Option[Float] = _

  @Expose(serialize = true)
  override var lightTransmission: Option[Float] = _

  @Expose(serialize = true)
  override var x: Option[Double] = _

  @Expose(serialize = true)
  override var y: Option[Double] = _

  @Expose(serialize = true)
  override var z: Option[Double] = _

  @Expose(serialize = true)
  override var posePositionUnits: String = _

  @Expose(serialize = true)
  override var phi: Option[Double] = _

  @Expose(serialize = true)
  override var theta: Option[Double] = _

  @Expose(serialize = true)
  override var psi: Option[Double] = _

  override val lastUpdated: Option[Instant] = None

  override def primaryKey = Option(uuid)

}

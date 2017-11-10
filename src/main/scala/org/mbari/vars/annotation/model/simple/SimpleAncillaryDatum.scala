package org.mbari.vars.annotation.model.simple

import java.util.UUID

import org.mbari.vars.annotation.model.CachedAncillaryDatum

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-07-11T14:55:00
 */
case class SimpleAncillaryDatum(
  uuid: UUID,
  latitude: Option[Double],
  longitude: Option[Double],
  depthMeters: Option[Float],
  salinity: Option[Float],
  temperatureCelsius: Option[Float],
  oxygenMlL: Option[Float],
  pressureDbar: Option[Float]
)

object SimpleAncillaryDatum {

  def apply(datum: CachedAncillaryDatum): SimpleAncillaryDatum =
    new SimpleAncillaryDatum(datum.uuid, datum.latitude, datum.longitude, datum.depthMeters,
      datum.salinity, datum.temperatureCelsius, datum.oxygenMlL, datum.pressureDbar)

}

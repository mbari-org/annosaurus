package org.mbari.vars.annotation.api

import org.mbari.vars.annotation.Constants
import org.mbari.vars.annotation.controllers.CachedAncillaryDatumController
import org.mbari.vars.annotation.model.simple.CachedAncillaryDatumBean

/**
  *
  *
  * @author Brian Schlining
  * @since 2017-11-13T16:26:00
  */
class CachedAncillaryDatumV1ApiSpec extends WebApiStack {

  private[this] val datumV1Api = {
    val controller = new CachedAncillaryDatumController(daoFactory)
    new CachedAncillaryDatumV1Api(controller)
  }
  protected[this] override val gson = Constants.GSON

  private[this] val path = "/v1/ancillarydata"
  addServlet(datumV1Api, path)

  "cachedAncillaryDatumV1Api" should "create" in {
    val data = (0 until 10).map(i =>
        {
          val d = new CachedAncillaryDatumBean
          d.latitude = Some(36 + i)
          d.longitude = Some(-122 + i)
          d.depthMeters = Some(100 * i)
          d.oxygenMlL = Some(0.23F * i)
          d
        })
    post(s"$path/bulk",
      
    )
  }

}

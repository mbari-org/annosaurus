package org.mbari.vars.annotation.controllers

import org.mbari.vars.annotation.dao.ObservationDAO
import org.mbari.vars.annotation.model.Observation

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-25T20:33:00
 */
class ObservationController(val daoFactory: BasicDAOFactory)
    extends BaseController[Observation, ObservationDAO[Observation]] {

  override def newDAO(): ObservationDAO[Observation] = daoFactory.newObservationDAO()
}

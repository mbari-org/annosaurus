package org.mbari.vars.annotation.dao

import org.mbari.vars.annotation.model.{ CachedVideoReferenceInfo, _ }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-25T17:45:00
 */
package object controllers {

  type BasicDAOFactory = DAOFactory[ImagedMoment, Observation, Association, ImageReference, CachedAncillaryDatum, CachedVideoReferenceInfo]
}

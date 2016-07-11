package org.mbari.vars.annotation

import org.mbari.vars.annotation.dao.DAOFactory
import org.mbari.vars.annotation.model.{ CachedVideoReferenceInfo, _ }

/**
 *
 *  Controllers abstract away the messier details of the DAO objects. Controllers are used by
 *  the api classes. You should be able to plug in any implementation of DAO's that you like.
 *  Although currently, I've only created a JPA/SQL version.
 *
 * @author Brian Schlining
 * @since 2016-06-25T17:45:00
 */
package object controllers {

  type BasicDAOFactory = DAOFactory[ImagedMoment, Observation, Association, ImageReference, CachedAncillaryDatum, CachedVideoReferenceInfo]
}

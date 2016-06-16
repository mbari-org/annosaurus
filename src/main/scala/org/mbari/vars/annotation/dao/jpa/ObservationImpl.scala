package org.mbari.vars.annotation.dao.jpa

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.model.{ Association, ImagedMoment, Observation }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-16T14:12:00
 */
class ObservationImpl extends Observation with JPAPersistentObject {

  override var concept: String = _
  override var duration: Duration = _
  override var imagedMoment: ImagedMoment = _
  override var observationDate: Instant = _
  override var observer: String = _

  override def addAssociation(association: Association): Unit = ???

  override def removeAssociation(association: Association): Unit = ???

  override def associations: Iterable[Association] = ???
}

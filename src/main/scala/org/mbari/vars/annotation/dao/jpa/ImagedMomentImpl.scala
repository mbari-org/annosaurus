package org.mbari.vars.annotation.dao.jpa

import java.time.{ Duration, Instant }
import java.util.UUID
import javax.persistence.{ Convert, _ }

import org.mbari.vars.annotation.model.{ ImagedMoment, Observation }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-16T14:12:00
 */
class ImagedMomentImpl extends ImagedMoment with JPAPersistentObject {

  @Column(
    name = "elapsed_time_millis",
    nullable = true
  )
  override var elapsedTime: Duration = _

  @Index(name = "idx_recorded_date", columnList = "recorded_date")
  @Column(
    name = "recorded_date",
    nullable = true
  )
  @Temporal(value = TemporalType.TIMESTAMP)
  @Convert(converter = classOf[InstantConverter])
  override var recordedDate: Instant = _

  override var timecode: String = _
  override var videoReferenceUUID: UUID = _

  override def addObservation(observation: Observation): Unit = ???

  override def removeObservation(observation: Observation): Unit = ???

  override def observations: Iterable[Observation] = ???
}

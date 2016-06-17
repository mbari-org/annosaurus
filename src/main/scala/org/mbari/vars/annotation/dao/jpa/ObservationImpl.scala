package org.mbari.vars.annotation.dao.jpa

import java.time.{ Duration, Instant }
import javax.persistence._
import java.util.{ ArrayList => JArrayList, List => JList }
import scala.collection.JavaConverters._

import org.mbari.vars.annotation.model.{ Association, ImagedMoment, Observation }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-16T14:12:00
 */
@Entity(name = "Observation")
@Table(name = "observations")
@EntityListeners(value = Array(classOf[TransactionLogger]))
class ObservationImpl extends Observation with JPAPersistentObject {

  @Index(name = "idx_concept", columnList = "concept")
  @Column(
    name = "concept",
    length = 256
  )
  override var concept: String = _

  @Column(
    name = "duration_millis",
    nullable = true
  )
  @Convert(converter = classOf[DurationConverter])
  override var duration: Duration = _

  @ManyToOne(cascade = Array(CascadeType.PERSIST, CascadeType.DETACH), optional = false)
  @JoinColumn(name = "imaged_moment_uuid", nullable = false)
  override var imagedMoment: ImagedMoment = _

  @Column(
    name = "observation_timestamp",
    nullable = true
  )
  @Temporal(value = TemporalType.TIMESTAMP)
  @Convert(converter = classOf[InstantConverter])
  override var observationDate: Instant = _

  @Column(
    name = "observer",
    length = 128,
    nullable = true
  )
  override var observer: String = _

  @OneToMany(
    targetEntity = classOf[Association],
    cascade = Array(CascadeType.ALL),
    fetch = FetchType.EAGER,
    mappedBy = "observation",
    orphanRemoval = true
  )
  protected var javaAssociations: JList[Association] = new JArrayList[Association]

  override def addAssociation(association: Association): Unit = {
    javaAssociations.add(association)
    association.observation = this
  }

  override def removeAssociation(association: Association): Unit = {
    javaAssociations.remove(association)
    association.observation = null
  }

  override def associations: Iterable[Association] = javaAssociations.asScala

}

object ObservationImpl {

  def apply(
    concept: String,
    duration: Option[Duration] = None,
    observationDate: Option[Instant] = None,
    observer: Option[String] = None
  ): ObservationImpl = {
    val obs = new ObservationImpl
    duration.foreach(obs.duration = _)
    observationDate.foreach(obs.observationDate = _)
    observer.foreach(obs.observer = _)
    obs
  }

}

package org.mbari.vars.annotation.dao.jpa

import java.util.UUID
import javax.persistence._

import org.mbari.vars.annotation.model.CachedVideoReferenceInfo

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T15:33:00
 */
@Entity(name = "VideoReferenceInfo")
@Table(name = "video_reference_information")
@EntityListeners(value = Array(classOf[TransactionLogger]))
class CachedVideoReferenceInfoImpl extends CachedVideoReferenceInfo with JPAPersistentObject {

  /**
   * typically this will be the chief scientist
   */
  @Column(
    name = "mission_contact",
    nullable = true,
    length = 64
  )
  override var missionContact: String = _

  @Column(
    name = "platform_name",
    nullable = false,
    length = 64
  )
  override var platformName: String = _

  @Column(
    name = "video_reference_uuid",
    nullable = false,
    unique = true
  )
  @Convert(converter = classOf[UUIDConverter])
  override var videoReferenceUUID: UUID = _

  @Column(
    name = "mission_id",
    nullable = false,
    length = 256
  )
  override var missionID: String = _

}

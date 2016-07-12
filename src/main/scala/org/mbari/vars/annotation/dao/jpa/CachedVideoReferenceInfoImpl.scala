package org.mbari.vars.annotation.dao.jpa

import java.util.UUID
import javax.persistence._

import com.google.gson.annotations.{ Expose, SerializedName }
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
  @Expose(serialize = true)
  @Column(
    name = "mission_contact",
    nullable = true,
    length = 64
  )
  override var missionContact: String = _

  @Expose(serialize = true)
  @Column(
    name = "platform_name",
    nullable = false,
    length = 64
  )
  override var platformName: String = _

  @Expose(serialize = true)
  @SerializedName(value = "video_reference_uuid")
  @Column(
    name = "video_reference_uuid",
    nullable = false,
    unique = true
  )
  @Convert(converter = classOf[UUIDConverter])
  override var videoReferenceUUID: UUID = _

  @Expose(serialize = true)
  @Column(
    name = "mission_id",
    nullable = false,
    length = 256
  )
  override var missionID: String = _

}

object CachedVideoReferenceInfoImpl {
  def apply(
    videoReferenceUUID: UUID,
    missionID: String,
    platformName: String,
    missionContact: Option[String] = None
  ): CachedVideoReferenceInfoImpl = {

    val d = new CachedVideoReferenceInfoImpl
    d.videoReferenceUUID = videoReferenceUUID
    d.missionID = missionID
    d.platformName = platformName
    missionContact.foreach(d.missionContact = _)
    d
  }
}

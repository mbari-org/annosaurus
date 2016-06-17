package org.mbari.vars.annotation.dao.jpa

import java.net.URL
import javax.persistence._

import org.mbari.vars.annotation.model.{ ImageReference, ImagedMoment }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T13:10:00
 */
@Entity(name = "ImageReference")
@Table(name = "image_references")
@EntityListeners(value = Array(classOf[TransactionLogger]))
class ImageReferenceImpl extends ImageReference with JPAPersistentObject {

  @Column(
    name = "description",
    length = 256,
    nullable = true
  )
  override var description: String = _

  @Column(
    name = "url",
    unique = true,
    length = 1024,
    nullable = false
  )
  @Convert(converter = classOf[URLConverter])
  override var url: URL = _

  @ManyToOne(cascade = Array(CascadeType.PERSIST, CascadeType.DETACH), optional = false)
  @JoinColumn(name = "imaged_moment_uuid", nullable = false)
  override var imagedMoment: ImagedMoment = _

  @Column(
    name = "height_pixels",
    nullable = true
  )
  override var height: Int = _

  @Column(
    name = "width_pixels",
    nullable = true
  )
  override var width: Int = _

  @Column(
    name = "format",
    length = 64,
    nullable = true
  )
  override var format: String = _
}

object ImageReferenceImpl {

  def apply(
    url: URL,
    width: Option[Int] = None,
    height: Option[Int] = None,
    format: Option[String] = None,
    description: Option[String] = None
  ): ImageReferenceImpl = {
    val i = new ImageReferenceImpl()
    i.url = url
    width.foreach(i.width = _)
    height.foreach(i.height = _)
    format.foreach(i.format = _)
    description.foreach(i.description = _)
    i
  }
}

/*
 * Copyright 2017 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.annosaurus.repository.jpa.entity

import com.google.gson.annotations.{Expose, SerializedName}
import jakarta.persistence._
import org.mbari.annosaurus.model.{MutableImageReference, MutableImagedMoment}
import org.mbari.annosaurus.repository.jpa.{JpaEntity, TransactionLogger, URLConverter}

import java.net.URL

/** @author
  *   Brian Schlining
  * @since 2016-06-17T13:10:00
  */
@Entity(name = "ImageReference")
@Table(
    name = "image_references",
    indexes = Array(
        new Index(name = "idx_image_references__url", columnList = "url"),
        new Index(
            name = "idx_image_references__imaged_moment_uuid",
            columnList = "imaged_moment_uuid"
        )
    )
)
@EntityListeners(value = Array(classOf[TransactionLogger]))
@NamedQueries(
    Array(
        new NamedQuery(
            name = "ImageReference.findAll",
            query = "SELECT r FROM ImageReference r ORDER BY r.url"
        ),
        new NamedQuery(
            name = "ImageReference.findByImageName",
            query = "SELECT r FROM ImageReference r WHERE TRIM(r.url) LIKE :name ORDER BY r.url"
        ),
        new NamedQuery(
            name = "ImageReference.findByURL",
            query = "SELECT r FROM ImageReference r WHERE r.url = :url ORDER BY r.url"
        )
    )
)
class ImageReferenceEntity extends MutableImageReference with JpaEntity {

    @Expose(serialize = true)
    @Column(name = "description", length = 256, nullable = true)
    var description: String = _

    @Expose(serialize = true)
    @Column(name = "url", unique = true, length = 1024, nullable = false)
    @Convert(converter = classOf[URLConverter])
    var url: URL = _

    @ManyToOne(
        cascade = Array(CascadeType.PERSIST, CascadeType.DETACH),
        optional = false,
        targetEntity = classOf[ImagedMomentEntity]
    )
    @JoinColumn(
        name = "imaged_moment_uuid",
        nullable = false,
        columnDefinition = "CHAR(36)"
    )
    var imagedMoment: MutableImagedMoment = _

    @Expose(serialize = true)
    @SerializedName(value = "height_pixels")
    @Column(name = "height_pixels", nullable = true)
    var height: Int = _

    @Expose(serialize = true)
    @SerializedName(value = "width_pixels")
    @Column(name = "width_pixels", nullable = true)
    var width: Int = _

    @Expose(serialize = true)
    @Column(name = "format", length = 64, nullable = true)
    var format: String = _
}

object ImageReferenceEntity {

    def apply(
        url: URL,
        width: Option[Int] = None,
        height: Option[Int] = None,
        format: Option[String] = None,
        description: Option[String] = None
    ): ImageReferenceEntity = {
        val i = new ImageReferenceEntity()
        i.url = url
        width.foreach(i.width = _)
        height.foreach(i.height = _)
        format.foreach(i.format = _)
        description.foreach(i.description = _)
        i
    }

    def apply(v: MutableImageReference): ImageReferenceEntity = {
        val i = new ImageReferenceEntity
        i.url = v.url
        i.description = v.description
        i.width = v.width
        i.height = v.height
        i.format = v.format
        i.uuid = v.uuid
        i
    }
}

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

package org.mbari.annosaurus.repository.jpa

import java.net.URL
import jakarta.persistence.EntityManager
import org.mbari.annosaurus.repository.ImageReferenceDAO
import org.mbari.annosaurus.repository.jpa.entity.ImageReferenceEntity

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-06-17T17:17:00
  */
class ImageReferenceDAOImpl(entityManager: EntityManager)
    extends BaseDAO[ImageReferenceEntity](entityManager)
    with ImageReferenceDAO[ImageReferenceEntity] {

  override def newPersistentObject(): ImageReferenceEntity = new ImageReferenceEntity

  override def newPersistentObject(
      url: URL,
      description: Option[String] = None,
      heightPixels: Option[Int] = None,
      widthPixels: Option[Int] = None,
      format: Option[String] = None
  ): ImageReferenceEntity = {
    val imageReference = newPersistentObject()
    imageReference.url = url
    description.foreach(imageReference.description = _)
    heightPixels.foreach(imageReference.height = _)
    widthPixels.foreach(imageReference.width = _)
    format.foreach(imageReference.format = _)
    imageReference
  }

  override def findAll(
      limit: Option[Int] = None,
      offset: Option[Int] = None
  ): Iterable[ImageReferenceEntity] =
    findByNamedQuery("ImageReference.findAll", limit = limit, offset = offset)

  override def findByURL(url: URL): Option[ImageReferenceEntity] =
    findByNamedQuery("ImageReference.findByURL", Map("url" -> url)).headOption

  override def findByImageName(name: String): Seq[ImageReferenceEntity] =
    findByNamedQuery("ImageReference.findByImageName", Map("name" -> s"%$name%"))

}

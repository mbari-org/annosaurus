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

package org.mbari.vars.annotation.dao

import java.net.URL

import org.mbari.vars.annotation.dao.jpa.ImageReferenceImpl
import org.mbari.vars.annotation.model.ImageReference

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-06-17T16:10:00
  */
trait ImageReferenceDAO[T <: ImageReference] extends DAO[T] {

  def newPersistentObject(
      url: URL,
      description: Option[String] = None,
      heightPixels: Option[Int] = None,
      widthPixels: Option[Int] = None,
      format: Option[String] = None
  ): T

  def findByURL(url: URL): Option[T]

  def findByImageName(name: String): Seq[ImageReferenceImpl]

}

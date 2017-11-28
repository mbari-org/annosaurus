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

package org.mbari.vars.annotation.model.simple

import java.net.URL
import java.util.UUID

import org.mbari.vars.annotation.model.ImageReference

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-07-11T14:59:00
 */
case class SimpleImageReference(uuid: UUID, url: URL, description: String,
  format: String, width: Int, height: Int)

object SimpleImageReference {

  def apply(imageReference: ImageReference): SimpleImageReference =
    new SimpleImageReference(imageReference.uuid, imageReference.url, imageReference.description,
      imageReference.format, imageReference.width, imageReference.height)

}
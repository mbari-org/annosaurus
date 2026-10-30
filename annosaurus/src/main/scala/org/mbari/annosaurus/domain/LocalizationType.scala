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

package org.mbari.annosaurus.domain

enum LocalizationType(val value: String):
    case BoundingBox extends LocalizationType("bounding_box")
    case Point       extends LocalizationType("point")
    case Line        extends LocalizationType("line")
    case Polygon     extends LocalizationType("polygon")
    case Mask        extends LocalizationType("mask")

object LocalizationType:

    val all: Seq[LocalizationType] = values.toSeq

    def fromValue(s: String): Either[String, LocalizationType] =
        all
            .find(_.value == s)
            .toRight(s"Invalid localization type '$s'. Expected one of: ${all.map(_.value).mkString(", ")}")

val MaskEncodings: Seq[String] = Seq("coco_rle")

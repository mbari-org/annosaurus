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

import java.net.URL
import java.time.Instant
import java.util.UUID

case class ImageCreateSC(
    video_reference_uuid: UUID,
    url: URL,
    timecode: Option[String] = None,
    elapsed_time_millis: Option[Long] = None,
    recorded_timestamp: Option[Instant] = None,
    format: Option[String] = None,
    width_pixels: Option[Int] = None,
    height_pixels: Option[Int] = None,
    description: Option[String] = None,
)

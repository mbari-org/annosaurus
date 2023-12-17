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

import java.time.Instant
import java.util.UUID

object DomainObjects {

    val associaiton = Association("foo", "bar", "baz", Some("some/stuff"), Some(UUID.randomUUID()))
    
    val observation   = Observation(
        "Grimpoteuthis",
        Some(123456L),
        Some("foo"),
        Some("bar"),
        Some("brian"),
        Some(Instant.now()),
        Seq(associaiton),
        Some(UUID.randomUUID()),
        None // Don't compare lastUpdated as it is set by the database
    )

    val ancillaryDatum = CachedAncillaryDatum(
        Some(-121.2),
        Some(36.2),
        Some(-100.2),
        Some(10.0),
        Some("some/stuff"),
        Some(34.567),
        Some(-5.678),
        Some(1.2455),
        Some(-99.999),
        Some(0.887),
        Some(-1.887),
        Some(2.887),
        Some(-3.887),
        Some("meters"),
        Some(2.33),
        Some(0.888),
        Some(1.23),
        Some(UUID.randomUUID()),
        None // Don't compare lastUpdated as it is set by the database
    )

    val imageReference = ImageReference(
        new java.net.URL("http://www.mbari.org"),
        Some("image/jpeg"),
        Some(100),
        Some(200),
        Some("A test image"),
        Some(java.util.UUID.randomUUID()),
        None // Don't compare lastUpdated as it is set by the database
    )

    val imagedMoment = ImagedMoment(
        UUID.randomUUID(),
        Some("01:23:45:22"),
        Some(12345L),
        Some(Instant.now()),
        Seq(observation),
        Seq(imageReference),
        Some(ancillaryDatum),
        Some(UUID.randomUUID()),
        None // Don't compare lastUpdated as it is set by the database
    )


}

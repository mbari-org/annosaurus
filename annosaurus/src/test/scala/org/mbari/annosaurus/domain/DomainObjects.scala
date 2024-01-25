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

    val association = Association("foo", "bar", "baz", Some("some/stuff"), Some(UUID.randomUUID()))

    val observation = Observation(
        "Grimpoteuthis",
        Some(123456L),
        Some("foo"),
        Some("bar"),
        Some("brian"),
        Some(Instant.now()),
        Seq(association),
        Some(UUID.randomUUID()),
        None // Don't compare lastUpdated as it is set by the database
    )

    val ancillaryDatum = CachedAncillaryDatum(
        Some(-121.2),
        Some(36.2),
        Some(-100.2f),
        Some(10.0f),
        Some("some/stuff"),
        Some(34.567f),
        Some(-5.678f),
        Some(1.2455f),
        Some(-99.999f),
        Some(0.887f),
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
        java.net.URI.create("http://www.mbari.org").toURL(),
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

    val annotation = Annotation(
        Some("transect"),
        Some(ancillaryDatum),
        Seq(association),
        Some("Grimpoteuthis"),
        Some(123456L),
        Some(12345L),
        Some("ROV:detailed"),
        Some(UUID.randomUUID()),
        Seq(imageReference),
        Some(Instant.now()),
        Some(UUID.randomUUID()),
        Some("brian"),
        Some(Instant.now()),
        Some("01:23:45:22"),
        Some(UUID.randomUUID())
    )

    val videoReferenceInfo = CachedVideoReferenceInfo(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Some("Ventana"),
        Some("Ventana 1234"),
        Some("Brian Schlining"),
        Some(Instant.now())
    )

    val index = Index(
        UUID.randomUUID(),
        Some("01:23:45:22"),
        Some(12345L),
        Some(Instant.now()),
        Some(UUID.randomUUID()),
        Some(Instant.now())
    )

    val queryConstraints = QueryConstraints(
        videoReferenceUuids = List(UUID.randomUUID(), UUID.randomUUID()),
        concepts = List("Grimpoteuthis"),
        observers = List("brian"),
        groups = List("ROV:detailed"),
        activities = List("transect"),
        minDepth = Some(10.0),
        maxDepth = Some(20.0),
        minLat = Some(36.2),
        maxLat = Some(36.3),
        minLon = Some(-121.2),
        maxLon = Some(-121.1),
        minTimestamp = Some(Instant.now()),
        maxTimestamp = Some(Instant.now()),
        linkName = Some("some/stuff"),
        linkValue = Some("some/stuff"),
        limit = Some(100),
        offset = Some(0),
        data = Some(true),
        missionContacts = List("Brian Schlining"),
        platformName = Some("Ventana"),
        missionId = Some("Ventana 1234")
    )

    val annotationUpdate = AnnotationUpdate(
        observationUuid = Some(UUID.randomUUID()),
        videoReferenceUuid = Some(UUID.randomUUID()),
        concept = Some("Grimpoteuthis"),
        observer = Some("brian"),
        observationTimestamp = Some(Instant.now()),
        timecode = Some("01:23:45:22"),
        elapsedTimeMillis = Some(12345L),
        recordedTimestamp = Some(Instant.now()),
        durationMillis = Some(9876L),
        group = Some("ROV:detailed"),
        activity = Some("transect"),
    )

}

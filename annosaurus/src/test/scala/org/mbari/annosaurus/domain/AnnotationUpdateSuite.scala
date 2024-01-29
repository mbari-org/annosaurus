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

class AnnotationUpdateSuite extends munit.FunSuite {

    val cc1 = DomainObjects.annotationUpdate

    test("camelCase/snake_case round trip") {
        val sc1 = cc1.toSnakeCase
        val cc2 = sc1.toCamelCase
        val sc2 = cc2.toSnakeCase
        assertEquals(sc1, sc2)
        assertEquals(cc1, cc2)
    }

    test("toAnnotation") {
        val a = cc1.toAnnotation
        assertEquals(cc1.observationUuid, a.observationUuid)
        assertEquals(cc1.videoReferenceUuid, a.videoReferenceUuid)
        assertEquals(cc1.concept, a.concept)
        assertEquals(cc1.observer, a.observer)
        assertEquals(cc1.observationTimestamp, a.observationTimestamp)
        assertEquals(cc1.timecode, a.timecode)
        assertEquals(cc1.elapsedTimeMillis, a.elapsedTimeMillis)
        assertEquals(cc1.recordedTimestamp, a.recordedTimestamp)
        assertEquals(cc1.durationMillis, a.durationMillis)
        assertEquals(cc1.group, a.group)
        assertEquals(cc1.activity, a.activity)
    }


}

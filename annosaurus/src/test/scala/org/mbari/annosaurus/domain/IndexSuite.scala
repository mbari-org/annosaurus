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

class IndexSuite extends munit.FunSuite {
  
    val cc1 = DomainObjects.index.copy(lastUpdated = None)

    test("camelCase/snake_case round trip") {
        val sc1 = cc1.toSnakeCase
        val cc2 = sc1.toCamelCase
        val sc2 = cc2.toSnakeCase
        assertEquals(cc2, cc1)
        assertEquals(sc1, sc2)
    }

    test("camelCase/Entity round trip") {
        val e1 = cc1.toEntity
        val cc2 = Index.from(e1).copy(lastUpdated = None)
        val e2 = cc2.toEntity
        assertEquals(cc2, cc1)
        // assertEquals(e1, e2)
        assertEquals(e2.getVideoReferenceUuid(), cc1.videoReferenceUuid)
        assertEquals(e2.getTimecode().toString(), cc1.timecode.get)
        assertEquals(Option(e2.getElapsedTime()), cc1.elapsedTime)
        assertEquals(Option(e2.getRecordedTimestamp()), cc1.recordedTimestamp)
        assertEquals(Option(e2.getUuid()), cc1.uuid)
    }
}

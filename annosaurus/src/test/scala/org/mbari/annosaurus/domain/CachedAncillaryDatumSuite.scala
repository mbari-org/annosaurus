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

import java.util.UUID
import java.time.Instant

// import org.mbari.annosaurus.controllers.TestUtils

class CachedAncillaryDatumSuite extends munit.FunSuite {

    val cc1 = DomainObjects.ancillaryDatum

    test("camelCase/snake_case round trip") {
        val sc1 = cc1.toSnakeCase
        val cc2 = sc1.toCamelCase
        val sc2 = cc2.toSnakeCase
        assertEquals(cc2, cc1)
        assertEquals(sc1, sc2)
    }
    
    test("camelCase/Entity round trip") {
        val e1 = cc1.toEntity
        val cc2 = CachedAncillaryDatum.from(e1).copy(lastUpdated = None)
        val e2 = cc2.toEntity
        assertEquals(cc2, cc1)
        // assertEquals(e1, e2)
    }

    test("double round/trip") {
        val cc2 = cc1.copy(depthMeters = None)
        val e1 = cc2.toEntity
        assertEquals(e1.getDepthMeters, null)
        val cc3 = CachedAncillaryDatum.from(e1)
        assertEquals(cc3.depthMeters, None)
        // assertEquals(e1, e2)
    }
}

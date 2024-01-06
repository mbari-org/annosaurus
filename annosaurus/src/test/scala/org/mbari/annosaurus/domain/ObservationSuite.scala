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

class ObservationSuite extends munit.FunSuite {

    val cc1   = DomainObjects.observation

    test("camelCase/snake_case round trip") {

        val sc1 = cc1.toSnakeCase
        val cc2 = sc1.toCamelCase
        val sc2 = cc2.toSnakeCase
        assertEquals(cc2, cc1)
        assertEquals(sc1, sc2)
        assertEquals(sc2.concept, cc1.concept)
        assertEquals(sc2.duration_millis, cc1.durationMillis)
        assertEquals(sc2.group, cc1.group)
        assertEquals(sc2.activity, cc1.activity)
        assertEquals(sc2.observer, cc1.observer)
        assertEquals(sc2.uuid, cc1.uuid)
        assertEquals(sc1.associations.size, 1)
        assertEquals(cc2.associations.size, 1)
    }

    test("camelCase/Entity round trip") {
        val e1 = cc1.toEntity
        val cc2 = Observation.from(e1, true)
        val e2 = cc2.toEntity
        assertEquals(cc2, cc1)
        assertEquals(e2.getConcept(), cc1.concept)
        assertEquals(e2.getDuration().toMillis, cc1.durationMillis.get)
        assertEquals(e2.getGroup(), cc1.group.get)
        assertEquals(e2.getActivity(), cc1.activity.get)
        assertEquals(e2.getObserver(), cc1.observer.get)
        assertEquals(e2.getUuid(), cc1.uuid.get)
        assertEquals(e2.getAssociations().size, 1)
        assertEquals(cc2.associations.size, 1)
    }

}

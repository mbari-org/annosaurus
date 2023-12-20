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

class AssociationSuite extends munit.FunSuite {

    val cc1 = DomainObjects.association

    test("camelCase/snake_case round trip") {
        
        val sc1 = cc1.toSnakeCase
        val cc2 = sc1.toCamelCase
        val sc2 = cc2.toSnakeCase
        assertEquals(cc2, cc1)
        assertEquals(sc1, sc2)
        assertEquals(sc2.link_name, cc1.linkName)
        assertEquals(sc2.to_concept, cc1.toConcept)
        assertEquals(sc2.link_value, cc1.linkValue)
        assertEquals(sc2.uuid, cc1.uuid)
    }

    test("camelCase/Entity round trip") {
        val e1 = cc1.toEntity
        val cc2 = Association.from(e1)
        val e2 = cc2.toEntity
        assertEquals(cc2, cc1)
//        assertEquals(e1, e2)
        assertEquals(e2.linkName, cc1.linkName)
        assertEquals(e2.toConcept, cc1.toConcept)
        assertEquals(e2.linkValue, cc1.linkValue)
        assertEquals(e2.uuid, cc1.uuid.get)
    }

}

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

package org.mbari.annosaurus.repository.query

import org.mbari.annosaurus.repository.query.PreparedStatementGenerator.{ColumnKind, typedValue}

import java.util.UUID

class PreparedStatementGeneratorSuite extends munit.FunSuite:

    test("boolean columns bind true/false/1/0 as booleans") {
        assertEquals(typedValue(Some(ColumnKind.Boolean), "true"), Some(java.lang.Boolean.TRUE))
        assertEquals(typedValue(Some(ColumnKind.Boolean), "FALSE"), Some(java.lang.Boolean.FALSE))
        assertEquals(typedValue(Some(ColumnKind.Boolean), "1"), Some(java.lang.Boolean.TRUE))
        assertEquals(typedValue(Some(ColumnKind.Boolean), "0"), Some(java.lang.Boolean.FALSE))
    }

    test("uuid columns bind a valid uuid as a UUID") {
        val u = UUID.randomUUID()
        assertEquals(typedValue(Some(ColumnKind.Uuid), u.toString), Some(u))
        assertEquals(typedValue(Some(ColumnKind.Uuid), u.toString.toUpperCase), Some(u))
    }

    test("unparseable values and untyped columns keep the string bind") {
        assertEquals(typedValue(Some(ColumnKind.Boolean), "yes"), None)
        assertEquals(typedValue(Some(ColumnKind.Uuid), "not-a-uuid"), None)
        assertEquals(typedValue(None, "true"), None)
        assertEquals(typedValue(None, UUID.randomUUID().toString), None)
    }

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

package org.mbari.annosaurus.repository.jdbc

opaque type JpqlSelect = String

object JpqlSelect:
    def apply(s: String): JpqlSelect =
        if !s.trim.toLowerCase.startsWith("select") then
            throw new IllegalArgumentException(s"Unexpected SELECT statement: $s")
        else s

opaque type JpqlFrom = String

object JpqlFrom:
    def apply(s: String): JpqlFrom =
        if !s.trim.toLowerCase.startsWith("from") then
            throw new IllegalArgumentException(s"Unexpected FROM statement: $s")
        else s

opaque type JpqlWhere = String

object JpqlWhere:
    def apply(s: String): JpqlWhere =
        if !s.trim.toLowerCase.startsWith("where") then
            throw new IllegalArgumentException(s"Unexpected WHERE statement: $s")
        else s

opaque type JpqlOrder = String

object JpqlOrder:
    def apply(s: String): JpqlOrder =
        if !s.trim.toLowerCase.startsWith("order by") then
            throw new IllegalArgumentException(s"Unexpected ORDER statement: $s")
        else s

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

package org.mbari.annosaurus.controllers

class LocalizationPagingSuite extends munit.FunSuite:

    import LocalizationController.pageLimit

    test("an absent limit defaults to 1000") {
        assertEquals(pageLimit(None), Some(1000))
    }

    test("a limit above 10000 is clamped to 10000, not rejected") {
        assertEquals(pageLimit(Some(20000)), Some(10000))
        assertEquals(pageLimit(Some(Int.MaxValue)), Some(10000))
    }

    test("limits up to the maximum are kept") {
        assertEquals(pageLimit(Some(1)), Some(1))
        assertEquals(pageLimit(Some(1000)), Some(1000))
        assertEquals(pageLimit(Some(10000)), Some(10000))
    }

    test("a negative limit is treated as absent") {
        assertEquals(pageLimit(Some(-5)), Some(1000))
    }

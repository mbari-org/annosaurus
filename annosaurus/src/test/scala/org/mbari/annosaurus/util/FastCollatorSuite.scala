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

package org.mbari.annosaurus.util

class FastCollatorSuite extends munit.FunSuite {

    test("collate") {
        //outside -2, outside -1, inside - 2, inside -1, match, inside + 1, inside + 2, outside + 1, outside + 2
        val a = Seq(8d, 9d, 13d, 14d, 15d, 16d, 17d, 21d, 22d)
        val b = Seq(10d, 15d, 20d)

        val f = FastCollator(a, b, 1d)

        val expected =
            Seq(None, Some(10d), None, Some(15.0), Some(15.0), Some(15.0), None, Some(20d), None)
        for (i <- f.indices) {
            val (_, actual) = f(i)
            assertEquals(actual, expected(i))
        }
    }


}

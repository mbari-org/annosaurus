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

package org.mbari.vars.annotation.math

import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers }

/**
 * @author Brian Schlining
 * @since 2018-05-31T11:33:00
 */
class FastCollatorSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  "FastCollator" should "collate" in {

    //outside -2, outside -1, inside - 2, inside -1, match, inside + 1, inside + 2, outside + 1, outside + 2
    val a = Seq(8D, 9D, 13D, 14D, 15D, 16D, 17D, 21D, 22D)
    val b = Seq(10D, 15D, 20D)

    val f = FastCollator(a, b, 1D)

    println(f)
    val expected = Seq(None, Some(10D), None, Some(15.0), Some(15.0),
      Some(15.0), None, Some(20D), None)
    for (i <- f.indices) {
      val (_, actual) = f(i)
      actual should be(expected(i))
    }
  }

}

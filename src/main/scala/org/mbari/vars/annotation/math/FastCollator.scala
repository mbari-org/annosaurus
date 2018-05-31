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

import org.slf4j.LoggerFactory
import scilube.Matlib

/**
 *
 *
 * @author Brian Schlining
 * @since 2015-03-02T16:20:00
 */
object FastCollator {

  private[this] val log = LoggerFactory.getLogger(getClass)

  def apply[A: Numeric, B: Numeric](
    a: Iterable[A],
    b: Iterable[B],
    tolerance: Double): Seq[(A, Option[B])] = {
    val numericA = implicitly[Numeric[A]]
    val numericB = implicitly[Numeric[B]]

    def fa(v: A) = numericA.toDouble(v)
    def fb(v: B) = numericB.toDouble(v)

    apply(a, fa, b, fb, tolerance)

  }

  def apply[A, B](
    a: Iterable[A],
    fnA: A => Double,
    b: Iterable[B],
    fnB: B => Double,
    tolerance: Double): Seq[(A, Option[B])] = {

    val xa = a.toSeq.sortBy(fnA) // sorted d0
    val xb = b.toSeq.sortBy(fnB) // sorted d1

    val na = xa.map(fnA).toArray // transformed d0 in same order as list0
    val nb = xb.map(fnB).toArray // transformed d1 in same order as list1

    val tmp = for {
      (va, ia) <- na.zipWithIndex
    } yield {
      val ib = Matlib.near(nb, va, inclusive = false)
      val nearest = if (ib < 0) None
      else {
        val vb = nb(ib)
        if (math.abs(va - vb) <= tolerance) Option(xb(ib)) else None
      }
      xa(ia) -> nearest
    }
    tmp.toSeq
  }

}
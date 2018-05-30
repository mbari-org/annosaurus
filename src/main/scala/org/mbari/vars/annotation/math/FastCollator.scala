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
    d0: Iterable[A],
    d1: Iterable[B],
    tolerance: Double): Seq[(A, Option[B])] = {
    val numericA = implicitly[Numeric[A]]
    val numericB = implicitly[Numeric[B]]

    def fa(v: A) = numericA.toDouble(v)
    def fb(v: B) = numericB.toDouble(v)

    apply(d0, fa, d1, fb, tolerance)

  }

  def apply[A, B](
    d0: Iterable[A],
    fn0: A => Double,
    d1: Iterable[B],
    fn1: B => Double,
    tolerance: Double): Seq[(A, Option[B])] = {

    val list0 = d0.toSeq.sortBy(fn0) // sorted d0
    val list1 = d1.toSeq.sortBy(fn1) // sorted d1

    val vals0 = list0.map(fn0).toArray // transformed d0 in same order as list0
    val vals1 = list1.map(fn1).toArray // transformed d1 in same order as list1

    val tmp = for {
      (val0, idx0) <- vals0.zipWithIndex
    } yield {
      val idx1 = Matlib.near(vals1, val0)
      val val1 = vals1(idx1)
      val nearest =
        if (math.abs(val0 - val1) <= tolerance) Option(list1(idx1))
        else None
      list0(idx0) -> nearest
    }
    tmp.toSeq
  }

}
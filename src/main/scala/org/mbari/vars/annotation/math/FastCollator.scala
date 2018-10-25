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

//import org.slf4j.LoggerFactory
import scilube.Matlib

/**
 *
 *
 * @author Brian Schlining
 * @since 2015-03-02T16:20:00
 */
object FastCollator {

  //private[this] val log = LoggerFactory.getLogger(getClass)

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
    d0: Iterable[A],
    fn0: A => Double,
    d1: Iterable[B],
    fn1: B => Double,
    tolerance: Double): Seq[(A, Option[B])] = {

    val list0 = d0.toSeq.sortBy(fn0) // sorted d0
    val list1 = d1.toSeq.sortBy(fn1) // sorted d1

    val vs0 = list0.map(fn0).toArray // transformed d0 in same order as list0
    val vs1 = list1.map(fn1).toArray // transformed d1 in same order as list1

    val tmp = for {
      (v0, i0) <- vs0.zipWithIndex
    } yield {
      val i1 = Matlib.near(vs1, v0)
      val nearest = if (i1 >= 0) {
        val v1 = vs1(i1)
        if (math.abs(v0 - v1) <= tolerance) Option(list1(i1))
        else None
      } else None
      list0(i0) -> nearest
    }
    tmp.toSeq
  }

}

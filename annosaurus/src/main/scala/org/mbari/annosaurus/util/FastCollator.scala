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

import org.mbari.scilube3.Matlib

import scala.math.Ordering.Double.IeeeOrdering

/**
 * @author
 *   Brian Schlining
 * @since 2015-03-02T16:20:00
 */
object FastCollator:

    def apply[A: Numeric, B: Numeric](
        a: Iterable[A],
        b: Iterable[B],
        tolerance: Double
    ): Seq[(A, Option[B])] =
        val numericA = implicitly[Numeric[A]]
        val numericB = implicitly[Numeric[B]]

        def fa(v: A) = numericA.toDouble(v)
        def fb(v: B) = numericB.toDouble(v)

        apply(a, fa, b, fb, tolerance)

    def apply[A, B](
        a: Iterable[A],
        fnA: A => Double,
        b: Iterable[B],
        fnB: B => Double,
        tolerance: Double
    ): Seq[(A, Option[B])] =

        val listA = a.toSeq.sortBy(fnA) // sorted d0
        val listB = b.toSeq.sortBy(fnB) // sorted d1

        val valuesA = listA.map(fnA).toArray // transformed d0 in same order as list0
        val valuesB = listB.map(fnB).toArray // transformed d1 in same order as list1

        val tmp =
            for (vA, iA) <- valuesA.zipWithIndex
            yield
                val iB      = Matlib.near(valuesB, vA, false)
                val nearest = if iB >= 0 then
                    val vB = valuesB(iB)
                    if math.abs(vA - vB) <= tolerance then Option(listB(iB))
                    else None
                else None
                listA(iA) -> nearest
        tmp.toSeq

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

package org.mbari.annosaurus.etc.jdk

import scala.util.Try

object Numbers:

    extension (obj: Object | Number)
        def asDouble: Option[Double] = Numbers.doubleConverter(obj)
        def asFloat: Option[Float]   = Numbers.floatConverter(obj)
        def asLong: Option[Long]     = Numbers.longConverter(obj)
        def asInt: Option[Int]       = Numbers.intConverter(obj)

    def doubleConverter(obj: Object | Number | Double): Option[Double] =
        obj match
            case null      => None
            case d: Double => Some(d)
            case n: Number => Some(n.doubleValue())
            case s: String => Try(s.toDouble).toOption
            case _         => None

    def floatConverter(obj: Object | Number | Float): Option[Float] =
        obj match
            case null      => None
            case f: Float  => Some(f)
            case n: Number => Some(n.floatValue())
            case s: String => Try(s.toFloat).toOption
            case _         => None

    def longConverter(obj: Object | Number | Long): Option[Long] =
        obj match
            case null      => None
            case l: Long   => Some(l)
            case n: Number => Some(n.longValue())
            case s: String => Try(s.toLong).toOption
            case _         => None

    def intConverter(obj: Object | Number | Int): Option[Int] =
        obj match
            case null      => None
            case i: Int    => Some(i)
            case n: Number => Some(n.intValue())
            case s: String => Try(s.toInt).toOption
            case _         => None

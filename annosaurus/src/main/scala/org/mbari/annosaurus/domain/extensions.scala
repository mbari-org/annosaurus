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

package org.mbari.annosaurus.domain

object extensions:

    // This is a workaround for Scala 3 treating java.lang.Number as a value class
    // resulting in Option(null) returning Some(0.0) instead of None
    extension (d: java.lang.Double)
        def toOption: Option[Double] =
            if (d == null) None
            else if (d.isNaN) None
            else Some(d)

    extension(f: java.lang.Float)
        def toOption: Option[Float] =
            if (f == null) None
            else if (f.isNaN) None
            else Some(f)

    extension (i: java.lang.Integer)
        def toOption: Option[Int] =
            if (i == null) None
            else Some(i)

    extension (l: java.lang.Long)
        def toOption: Option[Long] =
            if (l == null) None
            else Some(l)
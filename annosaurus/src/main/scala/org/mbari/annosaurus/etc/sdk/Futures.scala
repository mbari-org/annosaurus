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

package org.mbari.annosaurus.etc.sdk

import java.time.Duration as JDuration
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Futures:

    private val Timeout = Duration.apply(10, TimeUnit.SECONDS)

    /**
     * Join a future. (i.e. Await.result(future, Duration.Inf)
     *
     * @return
     *   The result of the future
     */
    extension [T](t: Future[T])
        def join: T                      = Await.result(t, Timeout)
        def join(duration: Duration): T  = Await.result(t, duration)
        def join(duration: JDuration): T = Await.result(t, Duration.fromNanos(duration.toNanos))

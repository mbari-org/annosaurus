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

package org.mbari.annosaurus.messaging

import scala.util.control.NonFatal

/**
 * @author
 *   Brian Schlining
 * @since 2020-01-30T15:57:00
 */
object Using:

    def apply[T <: AutoCloseable, V](r: => T)(f: T => V): V =
        val resource: T          = r
        require(resource != null, "resource is null")
        var exception: Throwable = null
        try f(resource)
        catch
            case e: Throwable =>
                exception = e
                throw e
        finally closeAndAddSuppressed(exception, resource)

    private def closeAndAddSuppressed(e: Throwable, resource: AutoCloseable): Unit =
        if e != null then
            try resource.close()
            catch
                case NonFatal(suppressed)            =>
                    e.addSuppressed(suppressed)
                case fatal: Throwable if NonFatal(e) =>
                    fatal.addSuppressed(e)
                    throw fatal
                case fatal: InterruptedException     =>
                    fatal.addSuppressed(e)
                    throw fatal
                case fatal: Throwable                =>
                    e.addSuppressed(fatal)
        else resource.close()

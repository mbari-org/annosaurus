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

package org.mbari.annosaurus.repository.jdbc

import java.time.Instant
import java.util.UUID
import scala.util.Try
import java.net.URL
import java.net.URI

extension (obj: Object)
    def asInstant: Option[Instant] = instantConverter(obj)
    def asInt: Option[Int]         = intConverter(obj)
    def asLong: Option[Long]       = longConverter(obj)
    def asString: Option[String]   = stringConverter(obj)
    def asUrl: Option[URL]         = urlConverter(obj)
    def asUUID: Option[UUID]       = uuidConverter(obj)

def instantConverter(obj: Object): Option[Instant] =
    obj match
        case null                            => None
        case ts: java.sql.Timestamp          => Some(ts.toInstant)
        case m: microsoft.sql.DateTimeOffset => Some(m.getOffsetDateTime().toInstant())
        case _                               => None // TODO handle postgres

def uuidConverter(obj: Object): Option[UUID] =
    obj match
        case null      => None
        case u: UUID   => Some(u)
        case s: String => Try(UUID.fromString(s)).toOption

def stringConverter(obj: Object): Option[String] =
    obj match
        case null      => None
        case s: String => Some(s)
        case o: Object => Some(o.toString)

def longConverter(obj: Object): Option[Long] =
    obj match
        case null      => None
        case n: Number => Some(n.longValue())
        case s: String => Try(s.toLong).toOption
        case _         => None

def intConverter(obj: Object): Option[Int] =
    obj match
        case null      => None
        case n: Number => Some(n.intValue())
        case s: String => Try(s.toInt).toOption
        case _         => None

def urlConverter(obj: Object): Option[URL] =
    obj match
        case null      => None
        case u: URL    => Some(u)
        case s: String => Try(URI.create(s).toURL()).toOption
        case _         => None

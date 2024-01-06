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
import java.net.URI
import java.net.URL

class SqlSuite extends munit.FunSuite {

    test("asString") {
        assert("foo".asString.get.contains("foo"))
        assert(Instant.now().asString.get.contains("T"))
        assert(UUID.randomUUID().asString.get.contains("-"))
        assert(URI.create("http://www.mbari.org").asString.get.contains("mbari"))
        assert(null.asString.isEmpty)
    }

    test("asUUID") {
        assert(UUID.randomUUID().asUUID.get.isInstanceOf[UUID])
        assert(UUID.randomUUID().toString().asUUID.get.isInstanceOf[UUID])
        assert("".asUUID.isEmpty)
        assert(null.asUUID.isEmpty)
    }

    test("asInstant") {
        val now = Instant.now()
        assert(now.asInstant.get.isInstanceOf[Instant])
        assert(java.sql.Timestamp.from(now).asInstant.get.isInstanceOf[Instant])
        assert(null.asInstant.isEmpty)
        assert("adfasdfasdf".asInstant.isEmpty)
    }

    test("asDouble") {
        assert(java.lang.Double.valueOf(1.0).asDouble.get.isInstanceOf[Double])
        assert(java.lang.Integer.valueOf(1).asDouble.get.isInstanceOf[Double])
        assert("1.0".asDouble.get.isInstanceOf[Double])
        assert(null.asDouble.isEmpty)
    }

    test("asLong") {
        assert(java.lang.Double.valueOf(1.0).asLong.get.isInstanceOf[Long])
        assert(java.lang.Integer.valueOf(1).asLong.get.isInstanceOf[Long])
        assert("1".asLong.get.isInstanceOf[Long])
        assert(null.asLong.isEmpty)
        assert("adfasdfasdf".asLong.isEmpty)
    }

    test("asInt") {
        assert(java.lang.Double.valueOf(1.0).asInt.get.isInstanceOf[Int])
        assert(java.lang.Integer.valueOf(1).asInt.get.isInstanceOf[Int])
        assert("1".asInt.get.isInstanceOf[Int])
        assert(null.asInt.isEmpty)
        assert("adfasdfasdf".asInt.isEmpty)
    }

    test("asUrl") {
        assert(URI.create("http://www.mbari.org").asUrl.get.isInstanceOf[URL])
        assert(null.asUrl.isEmpty)
        assert("adfasdfasdf".asUrl.isEmpty)
    }
  
}

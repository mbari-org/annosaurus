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
        assert(null.asUUID.isEmpty)
    }

    test("asInstant") {
        val now = Instant.now()
        assert(now.asInstant.get.isInstanceOf[Instant])
        assert(java.sql.Timestamp.from(now).asInstant.get.isInstanceOf[Instant])
        assert(null.asInstant.isEmpty)
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
    }

    test("asInt") {
        assert(java.lang.Double.valueOf(1.0).asInt.get.isInstanceOf[Int])
        assert(java.lang.Integer.valueOf(1).asInt.get.isInstanceOf[Int])
        assert("1".asInt.get.isInstanceOf[Int])
        assert(null.asInt.isEmpty)
    }

    test("asUrl") {
        assert(URI.create("http://www.mbari.org").asUrl.get.isInstanceOf[URL])
        assert(null.asUrl.isEmpty)
    }
  
}

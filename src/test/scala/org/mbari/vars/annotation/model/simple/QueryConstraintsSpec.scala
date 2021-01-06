package org.mbari.vars.annotation.model.simple

import org.mbari.vars.annotation.Constants
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.Instant
import java.util.UUID

class QueryConstraintsSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {
  private val gson = Constants.GSON

  "QueryConstraints" should "serialize to/from json" in {
    val concepts = List("foo", "bar", "baz")
    val qc = QueryConstraints(concepts)
    val json = gson.toJson(qc)
    println(json)
    val qc1 = QueryConstraints.fromJson(json)
    qc1.conceptSeq() should contain theSameElementsAs concepts

  }

  it should "serialize to/from more complicated json" in {
    val concepts = List("foo", "bar", "baz")
    val uuids = List(UUID.randomUUID(), UUID.randomUUID())
    val qc = QueryConstraints(concepts, uuids, Some(0), Some(90), Some(-20), Some(20), Some(Instant.EPOCH), Some(Instant.now()))
    val json = gson.toJson(qc)
    println(json)
    val qc1 = QueryConstraints.fromJson(json)
    qc1.conceptSeq() should contain theSameElementsAs concepts
  }

}

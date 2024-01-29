package org.mbari.annosaurus.domain

class IndexUpdateSuite extends munit.FunSuite {

    private val cc1 = {
        val a = DomainObjects.imagedMoment
        IndexUpdate(a.uuid.orNull, a.timecode, a.elapsedTimeMillis, a.recordedTimestamp)
    }

    test("camelCase/snake_case round trip") {
        val sc1 = cc1.toSnakeCase
        val cc2 = sc1.toCamelCase
        val sc2 = cc2.toSnakeCase
        assertEquals(cc1, cc2)
        assertEquals(sc1, sc2)
    }


}

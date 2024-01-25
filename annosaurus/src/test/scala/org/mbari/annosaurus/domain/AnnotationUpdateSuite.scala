package org.mbari.annosaurus.domain

class AnnotationUpdateSuite extends munit.FunSuite {

    val cc1 = DomainObjects.annotationUpdate

    test("camelCase/snake_case round trip") {
        val sc1 = cc1.toSnakeCase
        val cc2 = sc1.toCamelCase
        val sc2 = cc2.toSnakeCase
        assertEquals(sc1, sc2)
        assertEquals(cc1, cc2)
    }

    test("toAnnotation") {
        val a = cc1.toAnnotation
        assertEquals(cc1.observationUuid, a.observationUuid)
        assertEquals(cc1.videoReferenceUuid, a.videoReferenceUuid)
        assertEquals(cc1.concept, a.concept)
        assertEquals(cc1.observer, a.observer)
        assertEquals(cc1.observationTimestamp, a.observationTimestamp)
        assertEquals(cc1.timecode, a.timecode)
        assertEquals(cc1.elapsedTimeMillis, a.elapsedTimeMillis)
        assertEquals(cc1.recordedTimestamp, a.recordedTimestamp)
        assertEquals(cc1.durationMillis, a.durationMillis)
        assertEquals(cc1.group, a.group)
        assertEquals(cc1.activity, a.activity)
    }


}

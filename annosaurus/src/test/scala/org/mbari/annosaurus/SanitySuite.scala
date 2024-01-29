package org.mbari.annosaurus

class SanitySuite extends munit.FunSuite {

    test("sanity") {
        assertEquals(1, 1)
    }

    test("Option(java.lang.Float)") {
        val f: java.lang.Float = 1.0f
        val o: Option[java.lang.Float] = Option(f)
        assertEquals(o, Some(f))

        val o2: Option[java.lang.Float] = Option(null)
        assertEquals(o2, None)
    }
}

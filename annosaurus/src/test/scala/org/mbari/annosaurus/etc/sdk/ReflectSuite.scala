package org.mbari.annosaurus.etc.sdk


import scala.jdk.CollectionConverters.*

case class Foo(a: String, b: Int, c: Option[String] = None)

case class Bar(a: String, b: Int, c: Option[Foo])

class Baz(val a: String, val b: Int)

class ReflectSuite extends munit.FunSuite:

    test("Reflect.fromMap"):

        val m = Map("a" -> "hello", "b" -> 42, "c" -> "world")
        val foo = Reflect.fromMap[Foo](m)
        assertEquals(foo, Foo("hello", 42, Some("world")))

        val n = Map("a" -> "hello", "b" -> 43)
        val foo2 = Reflect.fromMap[Foo](n)
        assertEquals(foo2, Foo("hello", 43))

        val o = Map("a" -> "yo", "b" -> 44, "c" -> foo)
        val bar = Reflect.fromMap[Bar](o)
        assertEquals(bar, Bar("yo", 44, Some(foo)))

        val p = Map("a" -> "greetings", "b" -> 45)
        val baz = Reflect.fromMap[Baz](p)
        assertEquals(baz.a, "greetings")
        assertEquals(baz.b, 45)

    test("Reflect.fromMap should fail when required parameter is missing"):
        val m = Map("a" -> "hello", "c" -> "world")
        intercept[java.lang.IllegalArgumentException]:
            Reflect.fromMap[Foo](m)

    test("Reflect.toMap"):
        val foo = Foo("hello", 42, Some("world"))
        val m = Reflect.toMap(foo)
        assertEquals(m, Map("a" -> "hello", "b" -> 42, "c" -> Some("world")))

        val bar = Bar("yo", 44, Some(foo))
        val n = Reflect.toMap(bar)
        assertEquals(n, Map("a" -> "yo", "b" -> 44, "c" -> Some(foo)))

        val bar2 = Bar("yo", 44, None)
        val o = Reflect.toMap(bar2)
        assertEquals(o, Map("a" -> "yo", "b" -> 44, "c" -> None))

    test("Reflext.fromMap using Map[T, Object]"):
        val m = java
            .util
            .Map
            .of[String, Object]("a", "hello", "b", Integer.valueOf(42), "c", "world")

        val foo = Reflect.fromJavaMap[Foo](m)
        assertEquals(foo, Foo("hello", 42, Some("world")))


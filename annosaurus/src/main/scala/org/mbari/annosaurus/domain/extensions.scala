package org.mbari.annosaurus.domain

object extensions:

    // This is a workaround for Scala 3 treating java.lang.Number as a value class
    // resulting in Option(null) returning Some(0.0) instead of None
    extension (d: java.lang.Double)
        def toOption: Option[Double] =
            if (d == null) None
            else if (d.isNaN) None
            else Some(d)

    extension(f: java.lang.Float)
        def toOption: Option[Float] =
            if (f == null) None
            else if (f.isNaN) None
            else Some(f)

    extension (i: java.lang.Integer)
        def toOption: Option[Int] =
            if (i == null) None
            else Some(i)

    extension (l: java.lang.Long)
        def toOption: Option[Long] =
            if (l == null) None
            else Some(l)
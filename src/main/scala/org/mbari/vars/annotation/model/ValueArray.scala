package org.mbari.vars.annotation.model

import java.util.UUID

import com.google.gson.annotations.Expose

/**
 * This is just a class to help GSON generate a list of values from an array.
 * Note that GSON chokes on this and assigns a Java ArrayList to the values.
 * So don't used the generic ValueArray
 *
 * @author Brian Schlining
 * @since 2016-09-14T14:02:00
 */
class ValueArray[A: Manifest] {

  @Expose(serialize = true)
  var values: Array[A] = _

}

object ValueArray {
  def apply[A: Manifest](s: Array[A]): ValueArray[A] = {
    val n = new ValueArray[A]
    n.values = s
    n
  }
}

class StringArray {
  @Expose(serialize = true)
  var values: Array[String] = _
}

object StringArray {
  def apply(s: Array[String]): StringArray = {
    val n = new StringArray
    n.values = s
    n
  }
}

class UUIDArray {
  @Expose(serialize = true)
  var values: Array[UUID] = _
}

object UUIDArray {
  def apply(s: Array[UUID]): UUIDArray = {
    val n = new UUIDArray
    n.values = s
    n
  }
}
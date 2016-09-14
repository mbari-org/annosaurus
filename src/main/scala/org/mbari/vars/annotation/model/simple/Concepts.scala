package org.mbari.vars.annotation.model.simple

import com.google.gson.annotations.Expose

/**
 * This is just a class to help GSON generate a list of names from an array
 *
 * @author Brian Schlining
 * @since 2016-09-13T16:15:00
 */
class Concepts {

  @Expose(serialize = true)
  var names: Array[String] = _

}

object Concepts {
  def apply(s: Array[String]): Concepts = {
    val n = new Concepts
    n.names = s
    n
  }
}

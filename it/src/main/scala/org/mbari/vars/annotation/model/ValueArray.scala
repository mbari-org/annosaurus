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

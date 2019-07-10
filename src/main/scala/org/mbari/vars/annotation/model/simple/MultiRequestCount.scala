package org.mbari.vars.annotation.model.simple

import com.google.gson.annotations.Expose

/**
 * @author Brian Schlining
 * @since 2019-07-10T14:51:00
 */
class MultiRequestCount {

  @Expose(serialize = true)
  var multiRequest: MultiRequest = _

  @Expose(serialize = true)
  var count: Long = 0
}



object MultiRequestCount {
  def apply(multiRequest: MultiRequest, count: Long): MultiRequestCount = {
    val m = new MultiRequestCount
    m.multiRequest = multiRequest
    m.count = count
    m
  }
}
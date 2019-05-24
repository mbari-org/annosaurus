package org.mbari.vars.annotation.model.simple

import com.google.gson.annotations.Expose

/**
 * @author Brian Schlining
 * @since 2019-05-23T16:55:00
 */
class ConcurrentRequestCount {

  @Expose(serialize = true)
  var concurrentRequest: ConcurrentRequest = _

  @Expose(serialize = true)
  var count: Long = 0

}

object ConcurrentRequestCount {
  def apply(concurrentRequest: ConcurrentRequest, count: Long): ConcurrentRequestCount = {
    val c = new ConcurrentRequestCount
    c.concurrentRequest = concurrentRequest
    c.count = count
    c
  }
}
